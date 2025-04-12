import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Button, Typography, Box, TextField, Select, MenuItem, FormControl, InputLabel,
  Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle,
  Chip, CircularProgress, Alert
} from '@mui/material';
import { Add as AddIcon, Delete as DeleteIcon, Edit as EditIcon } from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const MatchList = () => {
  const [matches, setMatches] = useState([]);
  const [tournaments, setTournaments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [tournamentFilter, setTournamentFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [matchToDelete, setMatchToDelete] = useState(null);
  const [deleteInProgress, setDeleteInProgress] = useState(false);
  
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      // Fetch matches and tournaments in parallel
      const matchesPromise = axios.get(API_ENDPOINTS.MATCHES.GET_ALL);
      const tournamentsPromise = axios.get(API_ENDPOINTS.TOURNAMENTS.GET_ALL);
      
      const [matchesResponse, tournamentsResponse] = await Promise.all([
        matchesPromise,
        tournamentsPromise
      ]);
      
      setMatches(matchesResponse.data);
      setTournaments(tournamentsResponse.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching data:', err);
      setError('Error fetching data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddMatch = () => {
    navigate('/admin/matches/new');
  };

  const handleEditMatch = (id) => {
    navigate(`/admin/matches/${id}`);
  };

  const handleDeleteClick = (match) => {
    setMatchToDelete(match);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!matchToDelete) return;
    
    setDeleteInProgress(true);
    try {
      await axios.delete(API_ENDPOINTS.MATCHES.DELETE(matchToDelete.id));
      setMatches(prevMatches => prevMatches.filter(match => match.id !== matchToDelete.id));
      setDeleteDialogOpen(false);
      setMatchToDelete(null);
    } catch (err) {
      console.error('Error deleting match:', err);
      let errorMessage = 'Error deleting match. Please try again.';
      
      // Get more specific error message if available
      if (err.response?.data?.message) {
        errorMessage = err.response.data.message;
      }
      
      setError(errorMessage);
    } finally {
      setDeleteInProgress(false);
    }
  };

  const handleExportCsv = () => {
    try {
      // Get the selected tournament filter for exporting
      const tournamentParam = tournamentFilter ? `?tournamentId=${tournamentFilter}` : '';
      const url = `${API_ENDPOINTS.REPORTS.EXPORT_MATCHES_CSV}${tournamentParam}`;
      
      // Set authorization header for the request
      const token = localStorage.getItem('token');
      if (token) {
        // Create a hidden anchor element to download the file
        const a = document.createElement('a');
        a.style.display = 'none';
        document.body.appendChild(a);
        
        // Use XMLHttpRequest for binary data download with auth headers
        const xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        xhr.responseType = 'blob';
        
        xhr.onload = function() {
          if (this.status === 200) {
            const blob = new Blob([this.response], { type: 'text/csv' });
            const downloadUrl = URL.createObjectURL(blob);
            a.href = downloadUrl;
            a.download = 'matches.csv';
            a.click();
            URL.revokeObjectURL(downloadUrl);
          } else {
            console.error('Error exporting CSV:', this.statusText);
            setError('Error exporting data to CSV. Please try again.');
          }
        };
        
        xhr.onerror = function() {
          console.error('Error exporting CSV: Network error');
          setError('Network error when exporting data to CSV. Please try again.');
        };
        
        xhr.send();
        setTimeout(() => document.body.removeChild(a), 100);
      } else {
        setError('You need to be logged in to export data.');
      }
    } catch (err) {
      console.error('Error exporting CSV:', err);
      setError('Error exporting data to CSV. Please try again.');
    }
  };

  const handleExportTxt = () => {
    try {
      // Get the selected tournament filter for exporting
      const tournamentParam = tournamentFilter ? `?tournamentId=${tournamentFilter}` : '';
      const url = `${API_ENDPOINTS.REPORTS.EXPORT_MATCHES_TXT}${tournamentParam}`;
      
      // Set authorization header for the request
      const token = localStorage.getItem('token');
      if (token) {
        // Create a hidden anchor element to download the file
        const a = document.createElement('a');
        a.style.display = 'none';
        document.body.appendChild(a);
        
        // Use XMLHttpRequest for binary data download with auth headers
        const xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        xhr.responseType = 'blob';
        
        xhr.onload = function() {
          if (this.status === 200) {
            const blob = new Blob([this.response], { type: 'text/plain' });
            const downloadUrl = URL.createObjectURL(blob);
            a.href = downloadUrl;
            a.download = 'matches.txt';
            a.click();
            URL.revokeObjectURL(downloadUrl);
          } else {
            console.error('Error exporting TXT:', this.statusText);
            setError('Error exporting data to TXT. Please try again.');
          }
        };
        
        xhr.onerror = function() {
          console.error('Error exporting TXT: Network error');
          setError('Network error when exporting data to TXT. Please try again.');
        };
        
        xhr.send();
        setTimeout(() => document.body.removeChild(a), 100);
      } else {
        setError('You need to be logged in to export data.');
      }
    } catch (err) {
      console.error('Error exporting TXT:', err);
      setError('Error exporting data to TXT. Please try again.');
    }
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleStatusFilterChange = (e) => {
    setStatusFilter(e.target.value);
  };

  const handleTournamentFilterChange = (e) => {
    setTournamentFilter(e.target.value);
  };

  // Filter matches based on search term, status, and tournament filters
  const filteredMatches = matches.filter(match => {
    const matchesSearch = searchTerm === '' || 
      (match.player1Name && match.player1Name.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (match.player2Name && match.player2Name.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (match.refereeName && match.refereeName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (match.courtNumber && match.courtNumber.toString().includes(searchTerm));
    
    const matchesStatus = statusFilter === '' || match.status === statusFilter;
    const matchesTournament = tournamentFilter === '' || 
      (match.tournamentId && match.tournamentId.toString() === tournamentFilter);
    
    return matchesSearch && matchesStatus && matchesTournament;
  });

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return 'N/A';
    try {
      const date = new Date(dateTimeString);
      return date.toLocaleString();
    } catch (e) {
      return 'Invalid date';
    }
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'SCHEDULED': return 'info';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  // Check if a match can be safely deleted
  const canDeleteMatch = (match) => {
    // Only scheduled or cancelled matches can be deleted
    return match.status === 'SCHEDULED' || match.status === 'CANCELLED';
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
        <Typography variant="h4">Match Management</Typography>
        <Box>
          <Button 
            variant="outlined" 
            color="primary" 
            onClick={handleExportCsv}
            sx={{ mr: 1 }}
          >
            Export CSV
          </Button>
          <Button 
            variant="outlined" 
            color="primary" 
            onClick={handleExportTxt}
            sx={{ mr: 1 }}
          >
            Export TXT
          </Button>
          <Button 
            variant="contained" 
            color="primary" 
            startIcon={<AddIcon />}
            onClick={handleAddMatch}
          >
            Add Match
          </Button>
        </Box>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Box sx={{ display: 'flex', mb: 3, gap: 2 }}>
        <TextField
          label="Search Matches"
          variant="outlined"
          value={searchTerm}
          onChange={handleSearchChange}
          sx={{ flexGrow: 1 }}
        />
        
        <FormControl sx={{ minWidth: 150 }}>
          <InputLabel id="status-filter-label">Status</InputLabel>
          <Select
            labelId="status-filter-label"
            value={statusFilter}
            label="Status"
            onChange={handleStatusFilterChange}
          >
            <MenuItem value="">All Statuses</MenuItem>
            <MenuItem value="SCHEDULED">Scheduled</MenuItem>
            <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="CANCELLED">Cancelled</MenuItem>
          </Select>
        </FormControl>
        
        <FormControl sx={{ minWidth: 200 }}>
          <InputLabel id="tournament-filter-label">Tournament</InputLabel>
          <Select
            labelId="tournament-filter-label"
            value={tournamentFilter}
            label="Tournament"
            onChange={handleTournamentFilterChange}
          >
            <MenuItem value="">All Tournaments</MenuItem>
            {tournaments.map((tournament) => (
              <MenuItem key={tournament.id} value={tournament.id.toString()}>
                {tournament.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Tournament</TableCell>
              <TableCell>Players</TableCell>
              <TableCell>Date & Time</TableCell>
              <TableCell>Referee</TableCell>
              <TableCell>Court</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredMatches.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  No matches found
                </TableCell>
              </TableRow>
            ) : (
              filteredMatches.map((match) => (
                <TableRow key={match.id}>
                  <TableCell>{match.tournamentName || 'N/A'}</TableCell>
                  <TableCell>
                    {match.player1Name && match.player2Name 
                      ? `${match.player1Name} vs ${match.player2Name}` 
                      : 'Not assigned'}
                  </TableCell>
                  <TableCell>{formatDateTime(match.scheduledTime)}</TableCell>
                  <TableCell>{match.refereeName || 'Not assigned'}</TableCell>
                  <TableCell>{match.courtNumber || 'Not assigned'}</TableCell>
                  <TableCell>
                    <Chip 
                      label={match.status || 'Unknown'} 
                      color={getStatusColor(match.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Button
                      color="primary"
                      size="small"
                      startIcon={<EditIcon />}
                      onClick={() => handleEditMatch(match.id)}
                      sx={{ mr: 1 }}
                    >
                      Edit
                    </Button>
                    <Button
                      color="error"
                      size="small"
                      startIcon={<DeleteIcon />}
                      onClick={() => handleDeleteClick(match)}
                      disabled={!canDeleteMatch(match)}
                      title={!canDeleteMatch(match) ? "Only scheduled or cancelled matches can be deleted" : ""}
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
        onClose={() => !deleteInProgress && setDeleteDialogOpen(false)}
      >
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete this match? This action cannot be undone.
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
    </>
  );
};

export default MatchList;