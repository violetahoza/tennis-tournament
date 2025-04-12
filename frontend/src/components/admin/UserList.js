import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Button, Typography, Box, TextField, MenuItem, Select, FormControl, InputLabel,
  Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle,
  CircularProgress, Alert, Chip
} from '@mui/material';
import { Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const UserList = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [userTypeFilter, setUserTypeFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState(null);
  const [errorDialogOpen, setErrorDialogOpen] = useState(false);
const [errorDialogMessage, setErrorDialogMessage] = useState('');

  const navigate = useNavigate();

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await axios.get(API_ENDPOINTS.USERS.GET_ALL);
      setUsers(res.data);
      setError(null);
    } catch (err) {
      setError('Error fetching users. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddUser = () => {
    navigate('/admin/users/new');
  };

  const handleEditUser = (id) => {
    navigate(`/admin/users/${id}`);
  };

  const handleDeleteClick = (user) => {
    setUserToDelete(user);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      await axios.delete(API_ENDPOINTS.USERS.DELETE(userToDelete.id));
      setUsers(users.filter(user => user.id !== userToDelete.id));
      setDeleteDialogOpen(false);
      setUserToDelete(null);
    } catch (err) {
      console.error(err);
      console.error('Error deleting user:', err.response?.data);

      let errorMessage = 'Error deleting user. Cannot delete users involved in ongoing or upcoming tournaments.';
      
      if (err.response) {
        const responseMessage = err.response.data?.message || '';
        
        if (responseMessage.includes('referee') && responseMessage.includes('assigned')) {
          errorMessage = 'Cannot delete referee assigned to ongoing or upcoming matches.';
        } else if (responseMessage.includes('player') && responseMessage.includes('registered')) {
          errorMessage = 'Cannot delete player registered in ongoing tournaments.';
        } else if (responseMessage.includes('admin') && responseMessage.includes('last')) {
          errorMessage = 'Cannot delete the last admin user. At least one admin must remain.';
        } else if (responseMessage.includes('matches') || responseMessage.includes('tournaments')) {
          errorMessage = 'Cannot delete user with active tournament or match associations.';
        }
      }
      
      setErrorDialogMessage(errorMessage);
      setErrorDialogOpen(true);
    }
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleUserTypeFilterChange = (e) => {
    setUserTypeFilter(e.target.value);
  };

  // Filter users based on search term and user type filter
  const filteredUsers = users.filter(user => {
    const matchesSearch = searchTerm === '' || 
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      `${user.firstName} ${user.lastName}`.toLowerCase().includes(searchTerm.toLowerCase());
    
    const matchesUserType = userTypeFilter === '' || user.userType === userTypeFilter;
    
    return matchesSearch && matchesUserType;
  });

  const getUserTypeColor = (userType) => {
    switch(userType) {
      case 'ADMIN': return 'error';
      case 'PLAYER': return 'success';
      case 'REFEREE': return 'warning';
      default: return 'default';
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">User Management</Typography>
        <Button 
          variant="contained" 
          color="primary" 
          startIcon={<AddIcon />}
          onClick={handleAddUser}
        >
          Add User
        </Button>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Box sx={{ display: 'flex', mb: 3, gap: 2 }}>
        <TextField
          label="Search Users"
          variant="outlined"
          value={searchTerm}
          onChange={handleSearchChange}
          sx={{ flexGrow: 1 }}
        />
        
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel id="user-type-filter-label">Filter by Role</InputLabel>
          <Select
            labelId="user-type-filter-label"
            id="user-type-filter"
            value={userTypeFilter}
            label="Filter by Role"
            onChange={handleUserTypeFilterChange}
          >
            <MenuItem value="">All Roles</MenuItem>
            <MenuItem value="ADMIN">Admin</MenuItem>
            <MenuItem value="PLAYER">Player</MenuItem>
            <MenuItem value="REFEREE">Referee</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Username</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredUsers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No users found
                </TableCell>
              </TableRow>
            ) : (
              filteredUsers.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.id}</TableCell>
                  <TableCell>{user.username}</TableCell>
                  <TableCell>{`${user.firstName} ${user.lastName}`}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    <Chip 
                      label={user.userType} 
                      color={getUserTypeColor(user.userType)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Button
                      color="primary"
                      size="small"
                      startIcon={<EditIcon />}
                      onClick={() => handleEditUser(user.id)}
                      sx={{ mr: 1 }}
                    >
                      Edit
                    </Button>
                    <Button
                      color="error"
                      size="small"
                      startIcon={<DeleteIcon />}
                      onClick={() => handleDeleteClick(user)}
                    >
                      Delete
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Delete Confirmation Dialog */}
      <Dialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
      >
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete user "{userToDelete?.username}"?
          </DialogContentText>
          {userToDelete?.userType === 'REFEREE' && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Note: Referees assigned to matches cannot be deleted.
            </Alert>
          )}
          {userToDelete?.userType === 'PLAYER' && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Note: Players registered in tournaments cannot be deleted.
            </Alert>
          )}
          {userToDelete?.userType === 'ADMIN' && users.filter(u => u.userType === 'ADMIN').length === 1 && (
            <Alert severity="error" sx={{ mt: 2 }}>
              Warning: This is the last admin user. The system requires at least one admin.
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error"
            disabled={userToDelete?.userType === 'ADMIN' && users.filter(u => u.userType === 'ADMIN').length === 1}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Error Dialog */}
      <Dialog
        open={errorDialogOpen}
        onClose={() => setErrorDialogOpen(false)}
      >
        <DialogTitle>Error Deleting User</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {errorDialogMessage}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setErrorDialogOpen(false)} color="primary">
            OK
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default UserList;