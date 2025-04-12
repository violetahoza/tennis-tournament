import React, { useState, useContext } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import { Button, TextField, Paper, Typography, Container, Box, Alert, CircularProgress } from '@mui/material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';
import '../../styles/Auth.css'; 

const Login = () => {
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useContext(AuthContext);

  // Check if there's a message in location state (e.g., from registration)
  const message = location.state?.message || '';

  const onChange = e => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError(''); // Clear any previous errors when user types
  };

  const onSubmit = async e => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      console.log("Logging in with:", {
        username: formData.username,
        password: formData.password
      });

      const res = await axios.post(API_ENDPOINTS.AUTH.LOGIN, {
        username: formData.username,
        password: formData.password
      });

      console.log("Login response:", res.data);
      
      if (res.data && res.data.token) {
        // Store user data in localStorage
        localStorage.setItem('userData', JSON.stringify({
          id: res.data.id,
          username: res.data.username,
          email: res.data.email,
          userType: res.data.userType,
          firstName: res.data.firstName || '',
          lastName: res.data.lastName || ''
        }));
        
        // Store token and user data
        await login(res.data.token);
        
        // Redirect based on user type
        const userType = res.data.userType;
        if (userType === 'ADMIN') {
          navigate('/admin');
        } else if (userType === 'PLAYER') {
          navigate('/player');
        } else if (userType === 'REFEREE') {
          navigate('/referee');
        } else {
          navigate('/');
        }
      } else {
        setError('Invalid response from server');
      }
    } catch (err) {
      console.error("Login error:", err);
      
      const errorMessage = err.response?.data?.message || 
                          'Login failed. Please check your credentials.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <Container component="main" maxWidth="xs" className="auth-content">
        <Paper elevation={6} className="auth-paper">
          <div className="auth-header">
            <Typography component="h1" variant="h4" className="auth-title">
              Welcome Back
            </Typography>
            <Typography variant="subtitle1" className="auth-subtitle">
              Sign in to your tennis account
            </Typography>
          </div>
          
          {message && (
            <Alert severity="success" className="auth-alert">
              {message}
            </Alert>
          )}
          
          {error && (
            <Alert severity="error" className="auth-alert">
              {error}
            </Alert>
          )}
          
          <Box component="form" onSubmit={onSubmit} noValidate className="auth-form">
            <TextField
              margin="normal"
              required
              fullWidth
              id="username"
              label="Username"
              name="username"
              autoComplete="username"
              autoFocus
              value={formData.username}
              onChange={onChange}
              className="auth-input"
              variant="outlined"
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              autoComplete="current-password"
              value={formData.password}
              onChange={onChange}
              className="auth-input"
              variant="outlined"
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              className="auth-button"
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} className="auth-spinner" /> : 'Sign In'}
            </Button>
            
            <Typography variant="body2" className="auth-register-text">
              Don't have an account?{' '}
              <Button 
                color="secondary" 
                className="auth-register-button"
                onClick={() => navigate('/register')}
              >
                Create one
              </Button>
            </Typography>
          </Box>
        </Paper>
      </Container>
    </div>
  );
};

export default Login;