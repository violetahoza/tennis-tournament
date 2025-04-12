import React, { createContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { API_ENDPOINTS } from '../config';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [auth, setAuth] = useState({
    token: localStorage.getItem('token'),
    isAuthenticated: false,
    user: null,
    loading: true,
    error: null
  });

  const setAuthToken = (token) => {
    if (token) {
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      localStorage.setItem('token', token);
      console.log("Token set in axios headers:", token);
    } else {
      delete axios.defaults.headers.common['Authorization'];
      localStorage.removeItem('token');
      localStorage.removeItem('userData');
      console.log("Token removed from axios headers");
    }
  };

  const loadUser = useCallback(async () => {
    const token = localStorage.getItem('token');
    
    if (!token) {
      setAuth(prev => ({
        ...prev,
        isAuthenticated: false,
        user: null,
        loading: false
      }));
      return;
    }

    try {
      setAuth(prev => ({ ...prev, loading: true }));
      setAuthToken(token);
      
      // Use the token to authenticate and get user details
      // We'll extract user information from the token response stored during login
      const userData = JSON.parse(localStorage.getItem('userData'));
      
      if (userData) {
        // Use the stored user data from login response
        setAuth({
          token,
          isAuthenticated: true,
          user: {
            id: userData.id,
            username: userData.username,
            email: userData.email,
            userType: userData.userType,
            firstName: userData.firstName,
            lastName: userData.lastName
          },
          loading: false,
          error: null
        });
      } else {
        // Parse the JWT to get user information as a fallback
        // In production, we would make a request to the server to verify the token
        const tokenParts = token.split('.');
        if (tokenParts.length === 3) {
          try {
            const payload = JSON.parse(atob(tokenParts[1]));
            const user = {
              id: payload.id,
              username: payload.sub, // JWT subject is usually the username
              email: payload.email,
              userType: payload.userType || payload.role,
              firstName: payload.firstName,
              lastName: payload.lastName
            };
            
            setAuth({
              token,
              isAuthenticated: true,
              user,
              loading: false,
              error: null
            });
          } catch (parseError) {
            console.error('Error parsing token:', parseError);
            setAuthToken(null);
            setAuth({
              token: null,
              isAuthenticated: false,
              user: null,
              loading: false,
              error: 'Invalid token format. Please log in again.'
            });
          }
        }
      }
    } catch (err) {
      console.error('Load user error:', err.response?.data || err.message);
      // Token is invalid or expired
      setAuthToken(null);
      setAuth({
        token: null,
        isAuthenticated: false,
        user: null,
        loading: false,
        error: 'Session expired. Please log in again.'
      });
    }
  }, []);

  useEffect(() => {
    loadUser();
  }, [loadUser]);

  const login = async (token) => {
    setAuthToken(token);
    try {
      await loadUser();
      return true;
    } catch (error) {
      console.error("Login error:", error);
      return false;
    }
  };

  const logout = () => {
    setAuthToken(null);
    localStorage.removeItem('userData');
    setAuth({
      token: null,
      isAuthenticated: false,
      user: null,
      loading: false,
      error: null
    });
  };
  
  const clearError = () => {
    setAuth(prev => ({ ...prev, error: null }));
  };

  return (
    <AuthContext.Provider value={{
      auth,
      setAuth, // Export setAuth to allow direct updates
      login,
      logout,
      loadUser,
      clearError,
      setAuthError: (error) => setAuth(prev => ({ ...prev, error }))
    }}>
      {children}
    </AuthContext.Provider>
  );
};