import React, { useState } from 'react';
import { Routes, Route, useNavigate } from 'react-router-dom';
import { Box, Drawer, List, ListItem, ListItemIcon, ListItemText, AppBar, Toolbar, Typography, IconButton, Divider } from '@mui/material';
import { 
  Menu as MenuIcon, 
  SportsHandball as MatchIcon, 
  ScoreboardOutlined as ScoringIcon,
  Person as ProfileIcon, 
  Group as PlayersIcon,
  Logout as LogoutIcon 
} from '@mui/icons-material';
import { useContext } from 'react';
import { AuthContext } from '../../context/AuthContext';
import Notifications from '../common/Notifications';

// Referee Components
import MatchSchedule from './MatchSchedule';
import MatchScoring from './MatchScoring';
import Profile from './../common/Profile';
import PlayerList from './PlayerList';
import PlayerDetails from './PlayerDetails';

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
          Referee Dashboard
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        <ListItem button onClick={() => handleNavigation('/referee/matches')}>
          <ListItemIcon>
            <MatchIcon />
          </ListItemIcon>
          <ListItemText primary="My Matches" />
        </ListItem>
        <ListItem button onClick={() => handleNavigation('/referee/players')}>
          <ListItemIcon>
            <PlayersIcon />
          </ListItemIcon>
          <ListItemText primary="Player Directory" />
        </ListItem>
        <ListItem button onClick={() => handleNavigation('/referee/profile')}>
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
            Tennis Tournament - Referee
          </Typography>
          <Typography variant="subtitle1" sx={{ mr: 2 }}>
            Welcome, {auth.user?.firstName} {auth.user?.lastName}
          </Typography>
          
          {/* Add Notifications component */}
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
          <Route path="/" element={<MatchSchedule />} />
          <Route path="/matches" element={<MatchSchedule />} />
          <Route path="/matches/:id/score" element={<MatchScoring />} />
          <Route path="/players" element={<PlayerList />} />
          <Route path="/players/:id" element={<PlayerDetails />} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </Box>
    </Box>
  );
};

export default Dashboard;