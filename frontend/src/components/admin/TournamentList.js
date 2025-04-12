import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Button, Typography, Box, TextField, Select, MenuItem, FormControl, InputLabel,
  Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle,
  Chip, CircularProgress, IconButton, Snackbar
} from '@mui/material';
import { Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon, EmojiEvents as TournamentIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const TournamentList = () => {
  const [tournaments, setTournaments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [tournamentToDelete, setTournamentToDelete] = useState(null);
  const [deleteInProgress, setDeleteInProgress] = useState(false);
  const [errorDialogOpen, setErrorDialogOpen] = useState(false);
  const [snackbarOpen, setSnackbarOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  
  const navigate = useNavigate();

  useEffect(() => {
    fetchTournaments();
  }, []);

  const fetchTournaments = async () => {
    setLoading(true);
    try {
      const res = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_ALL);
      setTournaments(res.data);
      setError(null);
    } catch (err) {
      handleError('Error fetching tournaments. Please try again.', err);
    } finally {
      setLoading(false);
    }
  };

  const handleError = (message, err) => {
    console.error(err);
    setError(message);
    setErrorDialogOpen(true);
  };

  const handleSuccess = (message) => {
    setSnackbarMessage(message);
    setSnackbarOpen(true);
  };

  const handleAddTournament = () => {
    navigate('/admin/tournaments/new');
  };

  const handleEditTournament = (id) => {
    navigate(`/admin/tournaments/${id}`);
  };

  const handleDeleteClick = (tournament) => {
    setTournamentToDelete(tournament);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!tournamentToDelete) return;
    
    setDeleteInProgress(true);
    try {
      await axios.delete(API_ENDPOINTS.TOURNAMENTS.DELETE(tournamentToDelete.id));
      setTournaments(tournaments.filter(t => t.id !== tournamentToDelete.id));
      setDeleteDialogOpen(false);
      setTournamentToDelete(null);
      handleSuccess('Tournament deleted successfully');
    } catch (err) {
      let errorMessage = 'Error deleting tournament. Please try again.';
      
      if (err.response?.data?.message) {
        errorMessage = err.response.data.message;
      } else if (err.response?.data?.error) {
        errorMessage = err.response.data.error;
      }
      
      handleError(errorMessage, err);
    } finally {
      setDeleteInProgress(false);
    }
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value);
  };

  const handleCloseErrorDialog = () => {
    setErrorDialogOpen(false);
  };

  const handleCloseSnackbar = () => {
    setSnackbarOpen(false);
  };

  // Filter tournaments based on search term and status filter
  const filteredTournaments = tournaments.filter(tournament => {
    const matchesSearch = searchTerm === '' || 
      (tournament.name && tournament.name.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (tournament.location && tournament.location.toLowerCase().includes(searchTerm.toLowerCase()));
    
    let matchesStatus = true;
    if (statusFilter === 'OPEN') {
      matchesStatus = tournament.registrationOpen;
    } else if (statusFilter === 'CLOSED') {
      matchesStatus = !tournament.registrationOpen;
    }
    
    return matchesSearch && matchesStatus;
  });

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  const getRegistrationStatusColor = (isOpen) => {
    return isOpen ? 'success' : 'error';
  };

  const isUpcomingTournament = (tournament) => {
    return new Date(tournament.startDate) > new Date();
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
        <Typography variant="h4">Tournament Management</Typography>
        <Button 
          variant="contained" 
          color="primary" 
          startIcon={<AddIcon />}
          onClick={handleAddTournament}
        >
          Add Tournament
        </Button>
      </Box>

      <Box sx={{ display: 'flex', mb: 3, gap: 2 }}>
        <TextField
          label="Search Tournaments"
          variant="outlined"
          value={searchTerm}
          onChange={handleSearchChange}
          fullWidth
        />
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel id="status-filter-label">Registration Status</InputLabel>
          <Select
            labelId="status-filter-label"
            value={statusFilter}
            label="Registration Status"
            onChange={handleStatusFilterChange}
          >
            <MenuItem value="">All Statuses</MenuItem>
            <MenuItem value="OPEN">Registration Open</MenuItem>
            <MenuItem value="CLOSED">Registration Closed</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Location</TableCell>
              <TableCell>Start Date</TableCell>
              <TableCell>End Date</TableCell>
              <TableCell>Registration</TableCell>
              <TableCell>Players</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredTournaments.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  No tournaments found
                </TableCell>
              </TableRow>
            ) : (
              filteredTournaments.map((tournament) => (
                <TableRow key={tournament.id}>
                  <TableCell>{tournament.id}</TableCell>
                  <TableCell>{tournament.name}</TableCell>
                  <TableCell>{tournament.location}</TableCell>
                  <TableCell>{formatDate(tournament.startDate)}</TableCell>
                  <TableCell>{formatDate(tournament.endDate)}</TableCell>
                  <TableCell>
                    <Chip 
                      label={tournament.registrationOpen ? "Open" : "Closed"} 
                      color={getRegistrationStatusColor(tournament.registrationOpen)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{tournament.registeredPlayers}/{tournament.maxParticipants}</TableCell>
                  <TableCell>
                    <Button
                      color="primary"
                      size="small"
                      startIcon={<EditIcon />}
                      onClick={() => handleEditTournament(tournament.id)}
                      sx={{ mr: 1 }}
                    >
                      Edit
                    </Button>
                    <Button
                      color="error"
                      size="small"
                      startIcon={<DeleteIcon />}
                      onClick={() => handleDeleteClick(tournament)}
                      disabled={!isUpcomingTournament(tournament)}
                      title={!isUpcomingTournament(tournament) ? "Cannot delete tournaments that have already started" : ""}
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

      <Dialog
        open={deleteDialogOpen}
        onClose={() => !deleteInProgress && setDeleteDialogOpen(false)}
      >
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete tournament "{tournamentToDelete?.name}"? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)} disabled={deleteInProgress}>Cancel</Button>
          <Button 
            onClick={handleDeleteConfirm} 
            color="error" 
            disabled={deleteInProgress}
          >
            {deleteInProgress ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={errorDialogOpen}
        onClose={handleCloseErrorDialog}
      >
        <DialogTitle>Error</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {error}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseErrorDialog} color="primary">
            OK
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        message={snackbarMessage}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      />
    </>
  );
};

export default TournamentList;