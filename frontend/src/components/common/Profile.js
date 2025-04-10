import React, { useState, useEffect, useContext } from 'react';
import axios from 'axios';
import {
  Paper, Button, Typography, Box, Alert, CircularProgress,
  Grid, TextField, Card, CardContent, FormControl, InputLabel, Select, MenuItem
} from '@mui/material';
import { Save as SaveIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

/**
 * A reusable Profile component that can be used by all user types (Admin, Player, Referee).
 * It handles fetching and updating the user's profile information.
 */
const Profile = () => {
  const { auth, loadUser } = useContext(AuthContext);
  const userType = auth?.user?.userType || '';
  
  const [profile, setProfile] = useState({
    firstName: '',
    lastName: '',
    email: '',
    // Fields specific to user types
    handPreference: 'RIGHT', // For players
    certificationLevel: '', // For referees
    yearsOfExperience: 0 // For referees
  });
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    if (auth.user) {
      fetchProfile();
    } else {
      setLoading(false);
    }
  }, [auth]);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await axios.get(API_ENDPOINTS.USERS.GET_BY_ID(auth.user.id));
      const userData = res.data;
      
      console.log('Fetched user data:', userData);
      
      setProfile({
        firstName: userData.firstName || '',
        lastName: userData.lastName || '',
        email: userData.email || '',
        handPreference: userData.handPreference || 'RIGHT',
        certificationLevel: userData.certificationLevel || '',
        yearsOfExperience: userData.yearsOfExperience || 0
      });
      
      setError(null);
    } catch (err) {
      console.error('Error fetching profile:', err);
      setError('Error fetching profile. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setProfile({
      ...profile,
      [name]: name === 'yearsOfExperience' ? (parseInt(value, 10) || 0) : value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    setSaving(true);
    setError(null);
    setSuccess(null);
    
    try {
      // Create the update data object - only include fields that the API expects
      const updateData = {
        id: auth.user.id,
        username: auth.user.username,
        firstName: profile.firstName,
        lastName: profile.lastName,
        email: profile.email,
        userType: auth.user.userType
      };
      
      // Add user type specific fields
      if (userType === 'PLAYER') {
        updateData.handPreference = profile.handPreference;
      } else if (userType === 'REFEREE') {
        updateData.certificationLevel = profile.certificationLevel;
        updateData.yearsOfExperience = profile.yearsOfExperience;
      }
      
      console.log('Sending update data:', updateData);
      
      // Send the update request
      const res = await axios.put(API_ENDPOINTS.USERS.UPDATE(auth.user.id), updateData);
      
      console.log('Update response:', res.data);
      
      // Refresh the auth context with the new user data
      await loadUser();
      
      setSuccess('Profile updated successfully!');
      
    } catch (err) {
      console.error('Profile update error:', err);
      const errorMessage = err.response?.data?.message || 
                        err.response?.data || 
                        'Error updating profile. Please try again.';
      setError(typeof errorMessage === 'string' ? errorMessage : JSON.stringify(errorMessage));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!auth.user) {
    return (
      <Alert severity="error">
        You need to be logged in to view your profile.
      </Alert>
    );
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>My Profile</Typography>
      
      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}
      
      <form onSubmit={handleSubmit}>
        <Card sx={{ mb: 4 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>Personal Information</Typography>
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  name="firstName"
                  label="First Name"
                  fullWidth
                  value={profile.firstName}
                  onChange={handleChange}
                  required
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  name="lastName"
                  label="Last Name"
                  fullWidth
                  value={profile.lastName}
                  onChange={handleChange}
                  required
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  name="email"
                  label="Email"
                  type="email"
                  fullWidth
                  value={profile.email}
                  onChange={handleChange}
                  required
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  name="username"
                  label="Username"
                  fullWidth
                  value={auth.user.username}
                  InputProps={{ readOnly: true }}
                  disabled
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  name="userType"
                  label="Role"
                  fullWidth
                  value={auth.user.userType}
                  InputProps={{ readOnly: true }}
                  disabled
                />
              </Grid>
            </Grid>
          </CardContent>
        </Card>
        
        {/* Player specific fields */}
        {userType === 'PLAYER' && (
          <Card sx={{ mb: 4 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Player Information</Typography>
              <Typography variant="body2" color="textSecondary" paragraph>
                This information is used for tournament rankings and statistics.
              </Typography>
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <FormControl fullWidth>
                    <InputLabel id="handPreference-label">Hand Preference</InputLabel>
                    <Select
                      labelId="handPreference-label"
                      name="handPreference"
                      value={profile.handPreference}
                      label="Hand Preference"
                      onChange={handleChange}
                    >
                      <MenuItem value="RIGHT">Right Hand</MenuItem>
                      <MenuItem value="LEFT">Left Hand</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        )}
        
        {/* Referee specific fields */}
        {userType === 'REFEREE' && (
          <Card sx={{ mb: 4 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>Referee Information</Typography>
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="certificationLevel"
                    label="Certification Level"
                    fullWidth
                    value={profile.certificationLevel}
                    onChange={handleChange}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    name="yearsOfExperience"
                    label="Years of Experience"
                    type="number"
                    fullWidth
                    value={profile.yearsOfExperience}
                    onChange={handleChange}
                    inputProps={{ min: 0 }}
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        )}
        
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button
            type="submit"
            variant="contained"
            color="primary"
            startIcon={<SaveIcon />}
            disabled={saving}
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </Button>
        </Box>
      </form>
    </Paper>
  );
};

export default Profile;