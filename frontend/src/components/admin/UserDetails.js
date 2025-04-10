import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Box, Button, TextField, Typography, Paper, Grid, FormControl,
  InputLabel, Select, MenuItem, Alert, CircularProgress
} from '@mui/material';
import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const UserDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNewUser = id === 'new';

  const [user, setUser] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    userType: 'PLAYER',
    password: '',
    confirmPassword: ''
  });
  
  const [loading, setLoading] = useState(!isNewUser);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    if (!isNewUser) {
      fetchUser();
    }
  }, [id, isNewUser]);

  const fetchUser = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.USERS.GET_BY_ID(id));
      setUser({
        ...res.data,
        password: '',
        confirmPassword: ''
      });
      setError(null);
    } catch (err) {
      setError('Error fetching user details. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setUser({ ...user, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validation
    if (user.password !== user.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    
    setSaving(true);
    setError(null);
    setSuccess(null);
    
    try {
      let response;
      
      // Create new user or update existing user
      if (isNewUser) {
        const newUser = {
          username: user.username,
          email: user.email,
          password: user.password,
          firstName: user.firstName,
          lastName: user.lastName,
          userType: user.userType
        };
        
        response = await axios.post(API_ENDPOINTS.AUTH.REGISTER, newUser);
        setSuccess('User created successfully!');
        
        // Redirect to users list
        setTimeout(() => {
          navigate('/admin/users');
        }, 1500);
      } else {
        const userToUpdate = { ...user };
        // Only send password if it's provided
        if (!userToUpdate.password) {
          delete userToUpdate.password;
        }
        delete userToUpdate.confirmPassword;
        
        response = await axios.put(API_ENDPOINTS.USERS.UPDATE(id), userToUpdate);
        setSuccess('User updated successfully!');
        
        setUser({
          ...response.data,
          password: '',
          confirmPassword: ''
        });
      }
      
    } catch (err) {
      setError(err.response?.data?.message || 'Error saving user. Please try again.');
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleBack = () => {
    navigate('/admin/users');
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h5">
          {isNewUser ? 'Add New User' : `Edit User: ${user.username}`}
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      <form onSubmit={handleSubmit}>
        <Grid container spacing={3}>
          <Grid item xs={12} sm={6}>
            <TextField
              name="username"
              label="Username"
              fullWidth
              value={user.username}
              onChange={handleChange}
              disabled={!isNewUser}
              required
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              name="email"
              label="Email"
              type="email"
              fullWidth
              value={user.email}
              onChange={handleChange}
              required
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              name="firstName"
              label="First Name"
              fullWidth
              value={user.firstName}
              onChange={handleChange}
              required
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              name="lastName"
              label="Last Name"
              fullWidth
              value={user.lastName}
              onChange={handleChange}
              required
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <FormControl fullWidth>
              <InputLabel id="userType-label">Role</InputLabel>
              <Select
                labelId="userType-label"
                name="userType"
                value={user.userType}
                onChange={handleChange}
                label="Role"
                required
              >
                <MenuItem value="ADMIN">Admin</MenuItem>
                <MenuItem value="PLAYER">Player</MenuItem>
                <MenuItem value="REFEREE">Referee</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} sm={6}></Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              name="password"
              label={isNewUser ? "Password" : "New Password (leave blank to keep current)"}
              type="password"
              fullWidth
              value={user.password}
              onChange={handleChange}
              required={isNewUser}
              helperText="Password must be at least 8 characters and include uppercase, lowercase and digits"
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              name="confirmPassword"
              label="Confirm Password"
              type="password"
              fullWidth
              value={user.confirmPassword}
              onChange={handleChange}
              required={isNewUser || user.password}
            />
          </Grid>
          <Grid item xs={12}>
            <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
              <Button
                variant="contained"
                color="primary"
                type="submit"
                startIcon={<SaveIcon />}
                disabled={saving}
              >
                {saving ? 'Saving...' : 'Save User'}
              </Button>
            </Box>
          </Grid>
        </Grid>
      </form>
    </Paper>
  );
};

export default UserDetails;