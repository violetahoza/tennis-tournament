import React, { useState, useEffect, useContext, useCallback } from 'react';
import { 
  Badge, IconButton, Popover, List, ListItem, ListItemText, 
  Typography, Divider, Box, Button, CircularProgress 
} from '@mui/material';
import { Notifications as NotificationsIcon } from '@mui/icons-material';
import axios from 'axios';
import { AuthContext } from '../../context/AuthContext';
import { API_ENDPOINTS } from '../../config';

/**
 * Notifications component that polls for new notifications periodically
 */
const Notifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [anchorEl, setAnchorEl] = useState(null);
  const [loading, setLoading] = useState(false);
  const { auth } = useContext(AuthContext);
  
  // Function to fetch notifications - wrapped in useCallback to avoid recreating it on each render
  const fetchNotifications = useCallback(async () => {
    if (!auth.isAuthenticated || !auth.user?.id) return;
    
    try {
      setLoading(true);
      const response = await axios.get(API_ENDPOINTS.NOTIFICATIONS.GET_ALL);
      
      if (response.data && Array.isArray(response.data)) {
        // Create a map to ensure unique notifications by ID
        const notificationsMap = new Map();
        response.data.forEach(notification => {
          notificationsMap.set(notification.id, notification);
        });
        
        // Convert back to array and sort
        const uniqueNotifications = Array.from(notificationsMap.values()).sort((a, b) => 
          new Date(b.timestamp) - new Date(a.timestamp)
        );
        
        setNotifications(uniqueNotifications);
        setUnreadCount(uniqueNotifications.filter(n => !n.read).length);
      }
    } catch (error) {
      console.error('Error fetching notifications:', error);
    } finally {
      setLoading(false);
    }
  }, [auth.isAuthenticated, auth.user]);

  // Modify WebSocket handling to prevent duplicates
  useEffect(() => {
    if (!auth.isAuthenticated || !auth.user?.id) return;

    const socket = new WebSocket(`wss://your-api-endpoint/ws/notifications`);
    
    socket.onmessage = (event) => {
      const newNotification = JSON.parse(event.data);
      setNotifications(prev => {
        // Check if notification already exists
        const exists = prev.some(n => n.id === newNotification.id);
        if (!exists) {
          return [newNotification, ...prev].sort((a, b) => 
            new Date(b.timestamp) - new Date(a.timestamp)
          );
        }
        return prev;
      });
      
      // Update unread count if notification is new
      if (!newNotification.read) {
        setUnreadCount(prev => prev + 1);
      }
    };

    return () => socket.close();
  }, [auth.isAuthenticated, auth.user]);


  // Initial fetch of notifications
  useEffect(() => {
    if (auth.isAuthenticated && auth.user?.id) {
      fetchNotifications();
    }
  }, [auth.isAuthenticated, auth.user, fetchNotifications]);

  // Set up polling for new notifications
  useEffect(() => {
    if (!auth.isAuthenticated || !auth.user?.id) return;

    // Poll every 10 seconds
    const interval = setInterval(() => {
      fetchNotifications();
    }, 10000);

    return () => clearInterval(interval);
  }, [auth.isAuthenticated, auth.user, fetchNotifications]);

  // Mark a notification as read
  const markAsRead = async (notificationId) => {
    try {
      await axios.put(API_ENDPOINTS.NOTIFICATIONS.MARK_AS_READ(notificationId));
      
      setNotifications(prev => {
        const updated = prev.map(n => 
          n.id === notificationId ? { ...n, read: true } : n
        );
        return updated;
      });
      
      // Update unread count
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Error marking notification as read:', error);
    }
  };
  
  // Mark all notifications as read
  const markAllAsRead = async () => {
    try {
      await axios.put(API_ENDPOINTS.NOTIFICATIONS.MARK_ALL_AS_READ);
      
      setNotifications(prev => {
        const updated = prev.map(n => ({ ...n, read: true }));
        return updated;
      });
      
      // Reset unread count
      setUnreadCount(0);
    } catch (error) {
      console.error('Error marking all notifications as read:', error);
    }
  };
  
  // Handle click to open popover
  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
    // Refresh notifications when opening the popover
    fetchNotifications();
  };
  
  // Handle close popover
  const handleClose = () => {
    setAnchorEl(null);
  };
  
  // Handle notification click
  const handleNotificationClick = (notificationId) => {
    if (notifications.find(n => n.id === notificationId && !n.read)) {
      markAsRead(notificationId);
    }
    
    // Close the popover
    handleClose();
  };
  
  const open = Boolean(anchorEl);
  const id = open ? 'notifications-popover' : undefined;
  
  // Function to format notification timestamp
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    
    return date.toLocaleDateString();
  };
  
  // Get icon based on notification type
  const getNotificationIcon = (type) => {
    switch (type) {
      case 'MATCH_SCORE':
        return '🎾';
      case 'MATCH_COMPLETED':
        return '🏆';
      case 'MATCH_UPDATE':
        return '🎯';
      case 'TOURNAMENT_REGISTRATION':
        return '📝';
      case 'MATCH_SCHEDULED':
        return '📅';
      default:
        return '📣';
    }
  };
  
  // Get title based on notification type
  const getNotificationTitle = (type) => {
    switch (type) {
      case 'MATCH_SCORE':
        return 'Score Update';
      case 'MATCH_COMPLETED':
        return 'Match Completed';
      case 'MATCH_UPDATE':
        return 'Match Update';
      case 'TOURNAMENT_REGISTRATION':
        return 'Registration Update';
      case 'MATCH_SCHEDULED':
        return 'New Match Scheduled';
      default:
        return 'Notification';
    }
  };
  
  // For testing - sends a test notification
  const sendTestNotification = async () => {
    try {
      await axios.post(API_ENDPOINTS.NOTIFICATIONS.TEST, {
        userId: auth.user.id,
        type: 'MATCH_SCORE',
        message: 'This is a test notification',
        timestamp: new Date().toISOString(),
        read: false
      });
      
      // Refresh notifications after sending a test
      setTimeout(() => {
        fetchNotifications();
      }, 1000);
    } catch (error) {
      console.error('Error sending test notification:', error);
    }
  };
  
  return (
    <>
      <IconButton 
        color="inherit" 
        aria-label="notifications"
        onClick={handleClick}
      >
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>
      
      <Popover
        id={id}
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
      >
        <Box sx={{ width: 320, maxHeight: 400 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" p={2}>
            <Typography variant="h6">Notifications</Typography>
            <Box>
              {unreadCount > 0 && (
                <Button size="small" onClick={markAllAsRead} sx={{ mr: 1 }}>
                  Mark all as read
                </Button>
              )}
              {/* Uncomment for testing
              <Button size="small" variant="outlined" onClick={sendTestNotification}>
                Test
              </Button>
              */}
            </Box>
          </Box>
          
          <Divider />
          
          <List sx={{ width: '100%', maxHeight: 320, overflow: 'auto' }}>
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                <CircularProgress size={24} />
              </Box>
            ) : notifications.length === 0 ? (
              <ListItem>
                <ListItemText primary="No notifications" />
              </ListItem>
            ) : (
              notifications.map((notification) => (
                <React.Fragment key={notification.id}>
                  <ListItem 
                    alignItems="flex-start" 
                    onClick={() => handleNotificationClick(notification.id)}
                    sx={{ 
                      bgcolor: notification.read ? 'inherit' : 'action.hover',
                      cursor: 'pointer'
                    }}
                  >
                    <ListItemText
                      primary={
                        <Typography 
                          sx={{ 
                            display: 'block',
                            fontWeight: notification.read ? 'normal' : 'bold'
                          }}
                        >
                          {getNotificationIcon(notification.type)} {getNotificationTitle(notification.type)}
                        </Typography>
                      }
                      secondary={
                        <>
                          <Typography
                            component="span"
                            variant="body2"
                            color="text.primary"
                          >
                            {notification.message}
                          </Typography>
                          <Typography
                            component="div"
                            variant="caption"
                            color="text.secondary"
                            sx={{ mt: 0.5 }}
                          >
                            {formatTimestamp(notification.timestamp)}
                          </Typography>
                        </>
                      }
                    />
                  </ListItem>
                  <Divider />
                </React.Fragment>
              ))
            )}
          </List>
        </Box>
      </Popover>
    </>
  );
};

export default Notifications;