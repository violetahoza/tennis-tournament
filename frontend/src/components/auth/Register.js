import React, { useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { 
  Button, TextField, FormControl, InputLabel, Select, MenuItem,
  Paper, Typography, Container, Box, Alert, CircularProgress
} from '@mui/material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';
import '../../styles/Auth.css'; 

const Register = () => {
  const { login, auth, clearError } = useContext(AuthContext);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
    userType: 'PLAYER'
  });
  
  const [touchedFields, setTouchedFields] = useState({
    username: false,
    email: false,
    password: false,
    confirmPassword: false,
    firstName: false,
    lastName: false
  });
  
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [formSubmitted, setFormSubmitted] = useState(false);
  const navigate = useNavigate();

  const { username, email, password, confirmPassword, firstName, lastName, userType } = formData;

  useEffect(() => {
    // Clear any previous errors
    clearError();
    
    // If user is already authenticated, redirect to appropriate dashboard
    if (auth.isAuthenticated && !auth.loading) {
      redirectBasedOnRole(auth.user.userType);
    }
  }, [auth.isAuthenticated, auth.loading]);

  const redirectBasedOnRole = (userType) => {
    switch(userType) {
      case 'ADMIN':
        navigate('/admin');
        break;
      case 'PLAYER':
        navigate('/player');
        break;
      case 'REFEREE':
        navigate('/referee');
        break;
      default:
        navigate('/');
    }
  };

  // Validation rules
  const validateForm = () => {
    const errors = {};
    
    if (!username || username.length < 3) {
      errors.username = 'Username must be at least 3 characters long.';
    }
    
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
      errors.email = 'Please provide a valid email address.';
    }
    
    if (!password || password.length < 8) {
      errors.password = 'Password must be at least 8 characters long.';
    } else {
      // Check for the required password complexity (uppercase, lowercase, digit)
      const hasUppercase = /[A-Z]/.test(password);
      const hasLowercase = /[a-z]/.test(password);
      const hasDigit = /\d/.test(password);
      
      if (!hasUppercase) {
        errors.password = 'Password must contain at least one uppercase letter.';
      } else if (!hasLowercase) {
        errors.password = 'Password must contain at least one lowercase letter.';
      } else if (!hasDigit) {
        errors.password = 'Password must contain at least one digit.';
      }
    }
    
    if (!confirmPassword || password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match.';
    }
    
    if (!firstName || firstName.length < 2) {
      errors.firstName = 'First name must be at least 2 characters long.';
    }
    if (!lastName || lastName.length < 2) {
      errors.lastName = 'Last name must be at least 2 characters long.';
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const onChange = e => {
    const { name, value } = e.target;
    
    // Clear general error if it matches the current field
    if (error && error.toLowerCase().includes(name)) {
      setError('');
    }
    
    // Clear field-specific validation error
    if (validationErrors[name]) {
      setValidationErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
    
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Mark field as touched when changed
    if (!touchedFields[name]) {
      setTouchedFields(prev => ({
        ...prev,
        [name]: true
      }));
    }
  };

  const onBlur = (e) => {
    const { name } = e.target;
    // Mark field as touched when blurred (user leaves the field)
    if (!touchedFields[name]) {
      setTouchedFields(prev => ({
        ...prev,
        [name]: true
      }));
    }
    validateForm();
  };

  const shouldShowError = (fieldName) => {
    // Show error if:
    // - The form has been submitted, OR
    // - The field has been touched
    return formSubmitted || touchedFields[fieldName];
  };

  const onSubmit = async e => {
    e.preventDefault();
    setFormSubmitted(true);
    setLoading(true);
    setError('');

    // Validate form before submission
    if (!validateForm()) {
      setLoading(false);
      return;
    }

    try {
      console.log('Submitting registration to:', API_ENDPOINTS.AUTH.REGISTER);
      const registrationDto = {
        username,
        email,
        password,
        firstName,
        lastName,
        userType
      };
      console.log('Registration data:', registrationDto);

      // Register the user
      const registerResponse  = await axios.post(API_ENDPOINTS.AUTH.REGISTER, registrationDto);

      console.log('Registration successful:', registerResponse.data);
      //navigate('/login', { state: { message: 'Registration successful. You can now login.' } });

      // Automatically log in after successful registration
      const loginResponse = await axios.post(API_ENDPOINTS.AUTH.LOGIN, {
        username,
        password
      });
  
      if (loginResponse.data && loginResponse.data.token) {
        // Store user data in localStorage
        localStorage.setItem('userData', JSON.stringify({
          id: loginResponse.data.id,
          username: loginResponse.data.username,
          email: loginResponse.data.email,
          userType: loginResponse.data.userType,
          firstName: loginResponse.data.firstName || '',
          lastName: loginResponse.data.lastName || ''
        }));
        
        // Update auth context
        await login(loginResponse.data.token);
        
        // Redirect based on role
        redirectBasedOnRole(loginResponse.data.userType);
      } else {
        // If auto-login failed but registration succeeded
        navigate('/login', { 
          state: { 
            message: 'Registration successful. Please login with your credentials.' 
          } 
        });
      }
    } catch (err) {
      console.error('Registration error:', err);
      
      if (err.response) {
        // Handle different error messages
        const errorMessage = err.response.data?.message || String(err.response.data) || '';
        
        if (errorMessage.toLowerCase().includes('username')) {
          setValidationErrors(prev => ({
            ...prev,
            username: 'This username is already taken. Please choose another.'
          }));
        } else if (errorMessage.toLowerCase().includes('email')) {
          setValidationErrors(prev => ({
            ...prev,
            email: 'This email is already registered. Please use another email.'
          }));
        } else if (err.response.data?.errors) {
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
            } else {
              setError(errorMsg);
            }
          });
          
          setValidationErrors(prev => ({
            ...prev,
            ...newErrors
          }));
        } else {
          // Other errors
          setError(typeof errorMessage === 'string' ? errorMessage : JSON.stringify(errorMessage));
        }
      } else if (err.request) {
        // The request was made but no response was received
        setError('No response from server. Please check your connection and try again.');
      } else {
        // Something happened in setting up the request
        setError('An error occurred. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <Container component="main" maxWidth="sm" className="auth-content">
        <Paper elevation={6} className="auth-paper">
          <div className="auth-header">
            <Typography component="h1" variant="h4" className="auth-title">
              Create Your Account
            </Typography>
            <Typography variant="subtitle1" className="auth-subtitle">
              Join our tennis tournament community
            </Typography>
          </div>
          
          {/* Global Error Alert */}
          {error && (
            <Alert 
              severity="error" 
              className="auth-alert"
              onClose={() => setError('')}
            >
              {error}
            </Alert>
          )}
          
          <Box component="form" onSubmit={onSubmit} className="auth-form">
            <TextField
              margin="normal"
              required
              fullWidth
              id="username"
              label="Username"
              name="username"
              value={username}
              onChange={onChange}
              onBlur={onBlur}
              autoComplete="username"
              autoFocus
              error={(shouldShowError('username') && Boolean(validationErrors.username)) || 
                    (error && error.toLowerCase().includes('username'))}
              helperText={
                (shouldShowError('username') && validationErrors.username) || ' '
              }
              disabled={loading}
              className="auth-input"
            />
            
            <TextField
              margin="normal"
              required
              fullWidth
              id="email"
              label="Email Address"
              name="email"
              value={email}
              onChange={onChange}
              onBlur={onBlur}
              autoComplete="email"
              type="email"
              error={(shouldShowError('email') && Boolean(validationErrors.email)) || 
                    (error && error.toLowerCase().includes('email'))}
              helperText={
                (shouldShowError('email') && validationErrors.email) || ' '
              }
              disabled={loading}
              className="auth-input"
            />
            
            <div className="name-fields-container">
              <TextField
                margin="normal"
                required
                fullWidth
                id="firstName"
                label="First Name"
                name="firstName"
                value={firstName}
                onChange={onChange}
                onBlur={onBlur}
                error={shouldShowError('firstName') && Boolean(validationErrors.firstName)}
                helperText={shouldShowError('firstName') ? validationErrors.firstName : ' '}
                disabled={loading}
                className="auth-input name-field"
              />
              
              <TextField
                margin="normal"
                required
                fullWidth
                id="lastName"
                label="Last Name"
                name="lastName"
                value={lastName}
                onChange={onChange}
                onBlur={onBlur}
                error={shouldShowError('lastName') && Boolean(validationErrors.lastName)}
                helperText={shouldShowError('lastName') ? validationErrors.lastName : ' '}
                disabled={loading}
                className="auth-input name-field"
              />
            </div>
            
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              value={password}
              onChange={onChange}
              onBlur={onBlur}
              autoComplete="new-password"
              error={shouldShowError('password') && Boolean(validationErrors.password)}
              helperText={shouldShowError('password') ? 
                validationErrors.password : 
                'Password must have at least 8 characters'}
              disabled={loading}
              className="auth-input"
            />
            
            <TextField
              margin="normal"
              required
              fullWidth
              name="confirmPassword"
              label="Confirm Password"
              type="password"
              id="confirmPassword"
              value={confirmPassword}
              onChange={onChange}
              onBlur={onBlur}
              error={shouldShowError('confirmPassword') && Boolean(validationErrors.confirmPassword)}
              helperText={shouldShowError('confirmPassword') ? validationErrors.confirmPassword : ' '}
              disabled={loading}
              className="auth-input"
            />
            
            <FormControl fullWidth margin="normal" disabled={loading} className="auth-input">
              <InputLabel id="userType-label">Role</InputLabel>
              <Select
                labelId="userType-label"
                id="userType"
                name="userType"
                value={userType}
                onChange={onChange}
                label="Role"
              >
                <MenuItem value="PLAYER">Player</MenuItem>
                <MenuItem value="REFEREE">Referee</MenuItem>
              </Select>
            </FormControl>
            
            <Button
              type="submit"
              fullWidth
              variant="contained"
              className="auth-button"
              disabled={loading}
            >
              {loading ? <CircularProgress size={24} className="auth-spinner" /> : 'Register'}
            </Button>
            
            <Typography variant="body2" className="auth-switch-text">
              Already have an account?{' '}
              <Button 
                className="auth-switch-button"
                onClick={() => navigate('/login')} 
                disabled={loading}
              >
                Login
              </Button>
            </Typography>
          </Box>
        </Paper>
      </Container>
    </div>
  );
};

export default Register;