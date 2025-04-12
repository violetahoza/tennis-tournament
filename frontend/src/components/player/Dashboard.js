import React, { useState } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { Box, Drawer, List, ListItem, ListItemIcon, ListItemText, AppBar, Toolbar, Typography, IconButton, Divider } from '@mui/material';
import { 
  Menu as MenuIcon, 
  EmojiEvents as TournamentIcon, 
  SportsHandball as MatchIcon, 
  Person as ProfileIcon, 
  Logout as LogoutIcon 
} from '@mui/icons-material';
import { useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import Notifications from '../common/Notifications'; 

// Player Components
import TournamentList from './TournamentList';
import TournamentRegistration from './TournamentRegistration';
import MatchSchedule from './MatchSchedule';
import MatchDetails from './MatchDetails';
import Profile from './../common/Profile';

const Dashboard = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { auth, logout } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleNavigation = (path) => {
    navigate(path);
    setMobileOpen(false);
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const drawer = (
    <div>
      <Toolbar>
        <Typography variant="h6" noWrap component="div">
          Player Dashboard
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        <ListItem button onClick={() => handleNavigation('/player/tournaments')}>
          <ListItemIcon>
            <TournamentIcon />
          </ListItemIcon>
          <ListItemText primary="Tournaments" />
        </ListItem>
        <ListItem button onClick={() => handleNavigation('/player/matches')}>
          <ListItemIcon>
            <MatchIcon />
          </ListItemIcon>
          <ListItemText primary="My Matches" />
        </ListItem>
        <ListItem button onClick={() => handleNavigation('/player/profile')}>
          <ListItemIcon>
            <ProfileIcon />
          </ListItemIcon>
          <ListItemText primary="My Profile" />
        </ListItem>
      </List>
      <Divider />
      <List>
        <ListItem button onClick={handleLogout}>
          <ListItemIcon>
            <LogoutIcon />
          </ListItemIcon>
          <ListItemText primary="Logout" />
        </ListItem>
      </List>
    </div>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Tennis Tournament - Player
          </Typography>
          <Typography variant="subtitle1" sx={{ mr: 2 }}>
            Welcome, {auth.user.firstName || ''} {auth.user.lastName || ''}
          </Typography>
          
          {/* Add Notifications component here */}
          <Notifications />
          
          <IconButton color="inherit" onClick={handleLogout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { sm: 240 }, flexShrink: { sm: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true, // Better open performance on mobile.
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: 240 },
          }}
        >
          {drawer}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: 240 },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{ flexGrow: 1, p: 3, width: { sm: `calc(100% - 240px)` } }}
      >
        <Toolbar />
        <Routes>
          <Route path="/" element={<TournamentList />} />
          <Route path="/tournaments" element={<TournamentList />} />
          <Route path="/tournaments/:id" element={<TournamentRegistration />} />
          <Route path="/matches" element={<MatchSchedule />} />
          <Route path="/matches/:id" element={<MatchDetails />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </Box>
    </Box>
  );
};

export default Dashboard;