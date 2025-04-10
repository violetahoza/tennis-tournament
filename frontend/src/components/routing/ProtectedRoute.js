import React, { useContext } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { Box, CircularProgress } from '@mui/material';

const LoadingSpinner = () => (
  <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
    <CircularProgress />
  </Box>
);

const ProtectedRoute = ({ children, allowedRoles = [] }) => {
  const { auth } = useContext(AuthContext);
  const location = useLocation();

  // Show loading spinner while checking authentication
  if (auth.loading) {
    return <LoadingSpinner />;
  }

  // Redirect to login if not authenticated
  if (!auth.isAuthenticated || !auth.user) {
    // Save the attempted URL for redirecting after login
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // If roles are specified and user's role is not in the allowed roles, redirect
  if (allowedRoles.length > 0 && !allowedRoles.includes(auth.user.userType)) {
    // Redirect to appropriate dashboard based on userType
    switch (auth.user.userType) {
      case 'ADMIN':
        return <Navigate to="/admin" replace />;
      case 'PLAYER':
        return <Navigate to="/player" replace />;
      case 'REFEREE':
        return <Navigate to="/referee" replace />;
      default:
        return <Navigate to="/" replace />;
    }
  }

  // If everything is fine, render the protected component
  return children;
};

export default ProtectedRoute;