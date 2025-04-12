import React, { useState, useEffect } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Typography, FormControl, InputLabel, Select, MenuItem,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Paper, IconButton, Chip, Alert, CircularProgress
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import axios from 'axios';
import { API_ENDPOINTS } from '../../config';

const TournamentRegistrationEditor = ({ tournamentId, onStatusUpdated }) => {
  const [open, setOpen] = useState(false);
  const [registrations, setRegistrations] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const fetchRegistrations = async () => {
    if (!tournamentId) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const response = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(tournamentId));
      console.log("TournamentRegistrationEditor - fetched registrations:", response.data);
      setRegistrations(response.data);
    } catch (err) {
      console.error('Error fetching registrations:', err);
      setError('Failed to load registrations');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      fetchRegistrations();
    }
  }, [open, tournamentId]);

  const handleOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleStatusChange = async (registrationId, newStatus) => {
    try {
      setLoading(true);
      setSuccess(null);
      setError(null);
      
      await axios.put(
        API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.UPDATE_STATUS(registrationId),
        null,
        { params: { status: newStatus } }
      );
      
      // Update local state
      setRegistrations(registrations.map(reg => 
        reg.id === registrationId ? { ...reg, status: newStatus } : reg
      ));
      
      setSuccess(`Registration status updated to ${newStatus}`);
      
      // Notify parent component
      if (onStatusUpdated) {
        onStatusUpdated();
      }
    } catch (err) {
      console.error('Error updating registration status:', err);
      setError('Failed to update registration status');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickApprove = async (registrationId) => {
    handleStatusChange(registrationId, 'APPROVED');
  };

  const handleQuickReject = async (registrationId) => {
    handleStatusChange(registrationId, 'REJECTED');
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'PENDING': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'error';
      case 'WAITLISTED': return 'info';
      default: return 'default';
    }
  };

  return (
    <>
      <Button 
        variant="outlined" 
        color="primary" 
        onClick={handleOpen}
        disabled={!tournamentId}
      >
        Manage Registrations
      </Button>
      
      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>Tournament Registrations</DialogTitle>
        <DialogContent>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
          
          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}>
              <CircularProgress />
            </div>
          ) : registrations.length === 0 ? (
            <Typography>No registrations found for this tournament.</Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Player</TableCell>
                    <TableCell>Registration Date</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {registrations.map((registration) => (
                    <TableRow key={registration.id}>
                      <TableCell>{registration.playerName}</TableCell>
                      <TableCell>
                        {new Date(registration.registrationDate).toLocaleDateString()}
                      </TableCell>
                      <TableCell>
                        <Chip 
                          label={registration.status} 
                          color={getStatusColor(registration.status)}
                          size="small"
                        />
                      </TableCell>
                      <TableCell style={{ display: 'flex', gap: '8px' }}>
                        {registration.status === 'PENDING' && (
                          <>
                            <Button 
                              size="small" 
                              variant="outlined" 
                              color="success" 
                              onClick={() => handleQuickApprove(registration.id)}
                            >
                              Approve
                            </Button>
                            <Button 
                              size="small" 
                              variant="outlined" 
                              color="error" 
                              onClick={() => handleQuickReject(registration.id)}
                            >
                              Reject
                            </Button>
                          </>
                        )}
                        <FormControl size="small" sx={{ minWidth: 120 }}>
                          <InputLabel id={`status-label-${registration.id}`}>Change Status</InputLabel>
                          <Select
                            labelId={`status-label-${registration.id}`}
                            value=""
                            label="Change Status"
                            onChange={(e) => handleStatusChange(registration.id, e.target.value)}
                          >
                            <MenuItem value="PENDING">Pending</MenuItem>
                            <MenuItem value="APPROVED">Approved</MenuItem>
                            <MenuItem value="REJECTED">Rejected</MenuItem>
                            <MenuItem value="WAITLISTED">Waitlisted</MenuItem>
                          </Select>
                        </FormControl>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose} color="primary">
            Close
          </Button>
          <Button 
            onClick={fetchRegistrations} 
            color="primary"
            disabled={loading}
          >
            Refresh
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default TournamentRegistrationEditor;