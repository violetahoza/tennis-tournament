import React, { useState, useEffect, useContext, useCallback } from 'react';
import { 
  Badge, IconButton, Popover, List, ListItem, ListItemText, 
  Typography, Divider, Box, Button, CircularProgress, 
  ListItemIcon, Avatar, Tooltip
} from '@mui/material';
import { 
  Notifications as NotificationsIcon, 
  Scoreboard as ScoreboardIcon,
  EmojiEvents as TrophyIcon,
  Sports as SportsIcon,
  Assignment as AssignmentIcon,
  EventAvailable as EventIcon,
  Cancel as CancelIcon,
  HowToReg as ApprovalIcon,
  RemoveCircle as RejectionIcon
} from '@mui/icons-material';
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
        // More thorough duplicate check
        const isDuplicate = prev.some(n => 
          n.id === newNotification.id || 
          (n.type === newNotification.type && 
           n.message === newNotification.message && 
           Math.abs(new Date(n.timestamp) - new Date(newNotification.timestamp)) < 5000)
        );
        
        if (!isDuplicate) {
          return [newNotification, ...prev].sort((a, b) => 
            new Date(b.timestamp) - new Date(a.timestamp)
          );
        }
        return prev;
      });
      
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
      case 'MATCH_SCORE_UPDATED':
      case 'MATCH_SCORE_ADDED':
        return <ScoreboardIcon color="primary" />;
      case 'MATCH_COMPLETED':
        return <TrophyIcon color="success" />;
      case 'MATCH_SCHEDULED':
        return <EventIcon color="info" />;
      case 'MATCH_CANCELLED':
        return <CancelIcon color="error" />;
      case 'MATCH_ASSIGNMENT':
        return <AssignmentIcon color="secondary" />;
      case 'MATCH_STATUS_CHANGE':
        return <SportsIcon color="warning" />;
      case 'TOURNAMENT_REGISTRATION':
        return <EventIcon color="info" />;
      case 'REGISTRATION_APPROVED':
        return <ApprovalIcon color="success" />;
      case 'REGISTRATION_REJECTED':
        return <RejectionIcon color="error" />;
      case 'REGISTRATION_WAITLISTED':
        return <AssignmentIcon color="warning" />;
      case 'REGISTRATION_CANCELLED':
        return <CancelIcon color="error" />;
      default:
        return <NotificationsIcon color="action" />;
    }
  };
  
  // Get title based on notification type
  const getNotificationTitle = (type) => {
    switch (type) {
      case 'MATCH_SCORE':
      case 'MATCH_SCORE_UPDATED':
      case 'MATCH_SCORE_ADDED':
        return 'Score Update';
      case 'MATCH_COMPLETED':
        return 'Match Completed';
      case 'MATCH_STATUS_CHANGE':
        return 'Match Update';
      case 'MATCH_SCHEDULED':
        return 'New Match Scheduled';
      case 'MATCH_CANCELLED':
        return 'Match Cancelled';
      case 'MATCH_ASSIGNMENT':
        return 'Match Assignment';
      case 'TOURNAMENT_REGISTRATION':
        return 'Tournament Registration';
      case 'REGISTRATION_APPROVED':
        return 'Registration Approved';
      case 'REGISTRATION_REJECTED':
        return 'Registration Rejected';
      case 'REGISTRATION_WAITLISTED':
        return 'Registration Waitlisted';
      case 'REGISTRATION_CANCELLED':
        return 'Registration Cancelled';
      case 'REGISTRATION_STATUS_CHANGE':
        return 'Registration Status Change';
      default:
        return 'Notification';
    }
  };
  
  // Get background color based on notification type for visual distinction
  const getNotificationColor = (type) => {
    switch (type) {
      case 'MATCH_COMPLETED':
      case 'REGISTRATION_APPROVED':
        return '#e6f7e9'; // Light green
      case 'MATCH_CANCELLED':
      case 'REGISTRATION_REJECTED':
      case 'REGISTRATION_CANCELLED':
        return '#ffebee'; // Light red
      case 'MATCH_ASSIGNMENT':
        return '#e3f2fd'; // Light blue
      case 'REGISTRATION_WAITLISTED':
        return '#fff8e1'; // Light amber
      default:
        return 'transparent';
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