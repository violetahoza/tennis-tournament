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
  const [validationErrors, setValidationErrors] = useState({});
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    if (!isNewUser) {
      fetchUser();
    } else {
      setLoading(false);
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
    const { name, value } = e.target;
    setUser({ ...user, [name]: value });
    
    // Clear validation errors for this field
    if (validationErrors[name]) {
      setValidationErrors(prevErrors => ({
        ...prevErrors,
        [name]: null
      }));
    }
  };

  const validateForm = () => {
    const errors = {};
    
    // Required fields validation
    if (!user.username || user.username.length < 3) {
      errors.username = 'Username must be at least 3 characters long';
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!user.email || !emailRegex.test(user.email)) {
      errors.email = 'Please enter a valid email address';
    }
    
    if (!user.firstName || user.firstName.length < 2) {
      errors.firstName = 'First name must be at least 2 characters long';
    }
    
    if (!user.lastName || user.lastName.length < 2) {
      errors.lastName = 'Last name must be at least 2 characters long';
    }
    
    // Password validation for new users or password changes
    if (isNewUser || user.password) {
      if (user.password.length < 8) {
        errors.password = 'Password must be at least 8 characters long';
      } else {
        // Check for the required password complexity
        const hasUppercase = /[A-Z]/.test(user.password);
        const hasLowercase = /[a-z]/.test(user.password);
        const hasDigit = /\d/.test(user.password);
        
        if (!hasUppercase) {
          errors.password = 'Password must contain at least one uppercase letter';
        } else if (!hasLowercase) {
          errors.password = 'Password must contain at least one lowercase letter';
        } else if (!hasDigit) {
          errors.password = 'Password must contain at least one digit';
        }
      }
      
      if (user.password !== user.confirmPassword) {
        errors.confirmPassword = 'Passwords do not match';
      }
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Form validation
    if (!validateForm()) {
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
        // For existing users, only send password if it's provided
        const userToUpdate = { 
          id: user.id,
          username: user.username,
          email: user.email,
          firstName: user.firstName,
          lastName: user.lastName,
          userType: user.userType
        };
        
        // Only send password update if a new password is provided
        if (user.password) {
          userToUpdate.password = user.password;
        }
        
        response = await axios.put(API_ENDPOINTS.USERS.UPDATE(id), userToUpdate);
        setSuccess('User updated successfully!');
        
        setUser({
          ...response.data,
          password: '',
          confirmPassword: ''
        });
      }
      
    } catch (err) {
      const errorMessage = err.response?.data?.message || 'Error saving user. Please try again.';
      
      // Check for specific error types to display better messages
      if (err.response?.data?.errors) {
        // Handle validation errors from the server
        const serverErrors = err.response.data.errors;
        const newErrors = {};
        
        serverErrors.forEach(errorMsg => {
          if (errorMsg.toLowerCase().includes('username')) {
            newErrors.username = errorMsg;
          } else if (errorMsg.toLowerCase().includes('email')) {
            newErrors.email = errorMsg;
          } else if (errorMsg.toLowerCase().includes('password')) {
            newErrors.password = errorMsg;
          } else if (errorMsg.toLowerCase().includes('first name')) {
            newErrors.firstName = errorMsg;
          } else if (errorMsg.toLowerCase().includes('last name')) {
            newErrors.lastName = errorMsg;
          }
        });
        
        if (Object.keys(newErrors).length > 0) {
          setValidationErrors(prev => ({
            ...prev,
            ...newErrors
          }));
        } else {
          setError(errorMessage);
        }
      } else if (errorMessage.toLowerCase().includes('username')) {
        setValidationErrors(prev => ({
          ...prev,
          username: 'Username is already taken'
        }));
      } else if (errorMessage.toLowerCase().includes('email')) {
        setValidationErrors(prev => ({
          ...prev,
          email: 'Email is already in use'
        }));
      } else {
        setError(errorMessage);
      }
      
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
              error={Boolean(validationErrors.username)}
              helperText={validationErrors.username || ''}
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
              error={Boolean(validationErrors.email)}
              helperText={validationErrors.email || ''}
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
              error={Boolean(validationErrors.firstName)}
              helperText={validationErrors.firstName || ''}
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
              error={Boolean(validationErrors.lastName)}
              helperText={validationErrors.lastName || ''}
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
              error={Boolean(validationErrors.password)}
              helperText={validationErrors.password || "Password must be at least 8 characters and include uppercase, lowercase and digits"}
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
              error={Boolean(validationErrors.confirmPassword)}
              helperText={validationErrors.confirmPassword || ''}
              disabled={isNewUser ? false : !user.password}
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