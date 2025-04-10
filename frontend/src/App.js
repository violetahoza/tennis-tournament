import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/routing/ProtectedRoute';
import axios from 'axios';

// Auth Components
import Login from './components/auth/Login';
import Register from './components/auth/Register';

// Dashboard Components
import AdminDashboard from './components/admin/Dashboard';
import PlayerDashboard from './components/player/Dashboard';
import RefereeDashboard from './components/referee/Dashboard';

// Other Components
import NotFound from './components/layout/NotFound';

// Styles
import './App.css';

function App() {
  // Set up axios defaults
  useEffect(() => {
    // Add request interceptor for auth token
    const requestInterceptor = axios.interceptors.request.use(
      config => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      error => Promise.reject(error)
    );

    // Add response interceptor to handle token expiration
    const responseInterceptor = axios.interceptors.response.use(
      response => response,
      error => {
        if (error.response && error.response.status === 401) {
          // If 401 response returned from API, remove token and redirect to login
          localStorage.removeItem('token');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );

    // Clean up interceptors on component unmount
    return () => {
      axios.interceptors.request.eject(requestInterceptor);
      axios.interceptors.response.eject(responseInterceptor);
    };
  }, []);

  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* Protected Routes */}
          <Route 
            path="/admin/*" 
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AdminDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/player/*" 
            element={
              <ProtectedRoute allowedRoles={['PLAYER']}>
                <PlayerDashboard />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/referee/*" 
            element={
              <ProtectedRoute allowedRoles={['REFEREE']}>
                <RefereeDashboard />
              </ProtectedRoute>
            } 
          />

            {/* Redirect based on role */}
            <Route
            path="/dashboard"
            element={
              <ProtectedRoute allowedRoles={['ADMIN', 'PLAYER', 'REFEREE']}>
                {({ auth }) => {
                  if (auth.user?.userType === 'ADMIN') {
                    return <Navigate to="/admin" />;
                  } else if (auth.user?.userType === 'PLAYER') {
                    return <Navigate to="/player" />;
                  } else if (auth.user?.userType === 'REFEREE') {
                    return <Navigate to="/referee" />;
                  } else {
                    return <Navigate to="/login" />;
                  }
                }}
              </ProtectedRoute>
            }
            />
          {/* Redirect root to login or appropriate dashboard */}
          <Route 
            path="/" 
            element={<Navigate replace to="/login" />} 
          />

          {/* 404 Route */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;