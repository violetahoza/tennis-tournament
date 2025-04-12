import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Paper, Button, TextField, Typography, Box, Alert, CircularProgress,
  Grid, Card, CardContent, Tab, Tabs, List, ListItem, ListItemText, 
  Chip, Divider, Dialog, DialogTitle, DialogContent, DialogActions,
  MenuItem, Select, FormControl, InputLabel
} from '@mui/material';
import { 
  ArrowBack as ArrowBackIcon, 
  Save as SaveIcon, 
  Add as AddIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon 
} from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';

const TournamentDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const isNewTournament = id === 'new' || id === undefined;

  const [tournament, setTournament] = useState({
    name: '',
    description: '',
    location: '',
    startDate: '',
    endDate: '',
    registrationDeadline: '',
    maxParticipants: 32
  });
  
  const [registrations, setRegistrations] = useState([]);
  const [matches, setMatches] = useState([]);
  const [tabValue, setTabValue] = useState(0);
  
  const [loading, setLoading] = useState(!isNewTournament);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});
  
  // For registration status management
  const [registrationDialogOpen, setRegistrationDialogOpen] = useState(false);
  const [selectedRegistration, setSelectedRegistration] = useState(null);
  const [newStatus, setNewStatus] = useState('');
  const [statusSaving, setStatusSaving] = useState(false);

  useEffect(() => {
    if (!isNewTournament) {
      fetchTournamentData();
    } else {
      // For new tournaments, pre-populate dates with reasonable defaults
      const today = new Date();
      const startDate = new Date(today);
      startDate.setDate(today.getDate() + 30); // Default: Start in 30 days
      
      const endDate = new Date(startDate);
      endDate.setDate(startDate.getDate() + 7); // Default: 1 week duration
      
      const regDeadline = new Date(today);
      regDeadline.setDate(today.getDate() + 20); // Default: Registration closes 10 days before start
      
      setTournament({
        ...tournament,
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0],
        registrationDeadline: regDeadline.toISOString().split('T')[0],
      });
      
      // Clear error for new tournament (in case it was previously set)
      setError(null);
    }
  }, [id]);

  const fetchTournamentData = async () => {
    setLoading(true);
    try {
      const tournamentRes = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_BY_ID(id));
      
      // Format dates for input fields
      const tournamentData = {
        ...tournamentRes.data,
        startDate: formatDate(tournamentRes.data.startDate),
        endDate: formatDate(tournamentRes.data.endDate),
        registrationDeadline: formatDate(tournamentRes.data.registrationDeadline)
      };
      
      setTournament(tournamentData);
      
      // Fetch registrations
      try {
        const registrationsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(id));
        setRegistrations(registrationsRes.data);
      } catch (err) {
        console.error('Error fetching registrations:', err);
      }
      
      // Fetch matches
      try {
        const matchesRes = await axios.get(API_ENDPOINTS.MATCHES.GET_BY_TOURNAMENT(id));
        setMatches(matchesRes.data);
      } catch (err) {
        console.error('Error fetching matches:', err);
      }
      
    } catch (err) {
      setError('Error fetching tournament details. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setTournament({ ...tournament, [name]: value });
    
    // Clear validation error for this field if it exists
    if (validationErrors[name]) {
      setValidationErrors(prev => ({
        ...prev,
        [name]: null
      }));
    }
  };

  const validateForm = () => {
    const errors = {};
    
    // Required fields
    if (!tournament.name || tournament.name.trim() === '') {
      errors.name = 'Tournament name is required';
    }
    
    if (!tournament.location || tournament.location.trim() === '') {
      errors.location = 'Location is required';
    }
    
    if (!tournament.startDate) {
      errors.startDate = 'Start date is required';
    }
    
    if (!tournament.endDate) {
      errors.endDate = 'End date is required';
    }
    
    if (!tournament.registrationDeadline) {
      errors.registrationDeadline = 'Registration deadline is required';
    }
    
    if (!tournament.maxParticipants) {
      errors.maxParticipants = 'Maximum participants is required';
    } else if (tournament.maxParticipants < 2) {
      errors.maxParticipants = 'Maximum participants must be at least 2';
    }
    
    // Date validations
    if (tournament.startDate && tournament.endDate) {
      if (new Date(tournament.endDate) < new Date(tournament.startDate)) {
        errors.endDate = 'End date cannot be before start date';
      }
    }
    
    if (tournament.startDate && tournament.registrationDeadline) {
      if (new Date(tournament.registrationDeadline) > new Date(tournament.startDate)) {
        errors.registrationDeadline = 'Registration deadline cannot be after start date';
      }
    }
    
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate form
    if (!validateForm()) {
      return;
    }
    
    setSaving(true);
    setError(null);
    setSuccess(null);
    
    try {
      // Prepare the data for the API - ensure dates are in proper format
      const tournamentData = {
        ...tournament,
        // Ensure maxParticipants is a number
        maxParticipants: parseInt(tournament.maxParticipants, 10)
      };
      
      // Remove id field if creating a new tournament
      if (isNewTournament) {
        delete tournamentData.id;
      }
      
      console.log("Sending tournament data:", tournamentData);
      
      let response;
      
      // Create new tournament or update existing tournament
      if (isNewTournament) {
        response = await axios.post(API_ENDPOINTS.TOURNAMENTS.CREATE, tournamentData);
        setSuccess('Tournament created successfully!');
        
        // Redirect to the tournament list
        setTimeout(() => {
          navigate('/admin/tournaments');
        }, 1500);
      } else {
        response = await axios.put(API_ENDPOINTS.TOURNAMENTS.UPDATE(id), tournamentData);
        
        // Update local state with the response data
        const updatedTournament = {
          ...response.data,
          startDate: formatDate(response.data.startDate),
          endDate: formatDate(response.data.endDate),
          registrationDeadline: formatDate(response.data.registrationDeadline)
        };
        
        setTournament(updatedTournament);
        setSuccess('Tournament updated successfully!');
      }
      
    } catch (err) {
      console.error("Error saving tournament:", err);
      console.error("Response:", err.response);
      
      // Handle validation errors from server
      if (err.response?.data?.errors) {
        const serverErrors = {};
        err.response.data.errors.forEach(errorMsg => {
          if (errorMsg.toLowerCase().includes('name')) {
            serverErrors.name = errorMsg;
          } else if (errorMsg.toLowerCase().includes('location')) {
            serverErrors.location = errorMsg;
          } else if (errorMsg.toLowerCase().includes('start date')) {
            serverErrors.startDate = errorMsg;
          } else if (errorMsg.toLowerCase().includes('end date')) {
            serverErrors.endDate = errorMsg;
          } else if (errorMsg.toLowerCase().includes('registration deadline')) {
            serverErrors.registrationDeadline = errorMsg;
          } else if (errorMsg.toLowerCase().includes('maximum participants')) {
            serverErrors.maxParticipants = errorMsg;
          }
        });
        
        if (Object.keys(serverErrors).length > 0) {
          setValidationErrors(serverErrors);
        } else {
          setError(err.response?.data?.message || 'Error saving tournament. Please try again.');
        }
      } else {
        setError(err.response?.data?.message || 'Error saving tournament. Please try again.');
      }
    } finally {
      setSaving(false);
    }
  };

  const handleTabChange = (event, newValue) => {
    setTabValue(newValue);
  };

  const handleBack = () => {
    navigate('/admin/tournaments');
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    
    // Handle various date formats
    try {
      // If it's already in YYYY-MM-DD format, return it
      if (/^\d{4}-\d{2}-\d{2}$/.test(dateString)) {
        return dateString;
      }
      
      // If it includes time (YYYY-MM-DDTHH:MM:SS), extract just the date
      if (dateString.includes('T')) {
        return dateString.split('T')[0];
      }
      
      // Otherwise, try to parse and format it
      const date = new Date(dateString);
      return date.toISOString().split('T')[0];
    } catch (e) {
      console.error("Error formatting date:", e);
      return dateString; // Return original if parsing fails
    }
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

  const getMatchStatusColor = (status) => {
    switch(status) {
      case 'SCHEDULED': return 'info';
      case 'IN_PROGRESS': return 'warning';
      case 'COMPLETED': return 'success';
      case 'CANCELLED': return 'error';
      default: return 'default';
    }
  };

  // Open dialog to update registration status
  const handleOpenStatusDialog = (registration) => {
    setSelectedRegistration(registration);
    setNewStatus(registration.status);
    setRegistrationDialogOpen(true);
  };

  // Close registration status dialog
  const handleCloseStatusDialog = () => {
    setRegistrationDialogOpen(false);
    setSelectedRegistration(null);
    setNewStatus('');
  };

  // Handle status change in dialog
  const handleStatusChange = (e) => {
    setNewStatus(e.target.value);
  };

  // Submit registration status update
  const handleUpdateRegistrationStatus = async () => {
    if (!selectedRegistration || !newStatus) return;
    
    setStatusSaving(true);
    try {
      await axios.put(
        API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.UPDATE_STATUS(selectedRegistration.id),
        {},
        { params: { status: newStatus } }
      );
      
      // Refresh registrations
      const registrationsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(id));
      setRegistrations(registrationsRes.data);
      
      setSuccess(`Registration status updated to ${newStatus}`);
      handleCloseStatusDialog();
    } catch (error) {
      setError('Error updating registration status: ' + (error.response?.data?.message || error.message));
      console.error(error);
    } finally {
      setStatusSaving(false);
    }
  };

  // Quick approve/reject functions
  const handleQuickApprove = async (registration) => {
    try {
      await axios.put(
        API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.UPDATE_STATUS(registration.id),
        {},
        { params: { status: 'APPROVED' } }
      );
      
      // Refresh registrations
      const registrationsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(id));
      setRegistrations(registrationsRes.data);
      
      setSuccess(`Registration for ${registration.playerName} approved!`);
    } catch (error) {
      setError('Error approving registration: ' + (error.response?.data?.message || error.message));
      console.error(error);
    }
  };

  const handleQuickReject = async (registration) => {
    try {
      await axios.put(
        API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.UPDATE_STATUS(registration.id),
        {},
        { params: { status: 'REJECTED' } }
      );
      
      // Refresh registrations
      const registrationsRes = await axios.get(API_ENDPOINTS.TOURNAMENT_REGISTRATIONS.GET_BY_TOURNAMENT(id));
      setRegistrations(registrationsRes.data);
      
      setSuccess(`Registration for ${registration.playerName} rejected!`);
    } catch (error) {
      setError('Error rejecting registration: ' + (error.response?.data?.message || error.message));
      console.error(error);
    }
  };

  // Navigate to add new match with pre-selected tournament
  const handleAddMatch = () => {
    navigate('/admin/matches/new', { state: { tournamentId: id } });
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={handleBack}
          sx={{ mr: 2 }}
        >
          Back
        </Button>
        <Typography variant="h5">
          {isNewTournament ? 'Create New Tournament' : `Edit Tournament: ${tournament.name}`}
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

      {/* Only show tabs when editing an existing tournament, not when creating a new one */}
      {!isNewTournament && (
        <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="tournament details tabs">
            <Tab label="Tournament Details" />
            <Tab label="Registrations" />
            <Tab label="Matches" />
          </Tabs>
        </Box>
      )}

      {/* Show tournament form for new tournaments or when on the details tab */}
      {(isNewTournament || tabValue === 0) && (
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <TextField
                name="name"
                label="Tournament Name"
                fullWidth
                value={tournament.name}
                onChange={handleChange}
                error={Boolean(validationErrors.name)}
                helperText={validationErrors.name || ''}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                name="description"
                label="Description"
                fullWidth
                multiline
                rows={4}
                value={tournament.description || ''}
                onChange={handleChange}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                name="location"
                label="Location"
                fullWidth
                value={tournament.location}
                onChange={handleChange}
                error={Boolean(validationErrors.location)}
                helperText={validationErrors.location || ''}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                name="startDate"
                label="Start Date"
                type="date"
                fullWidth
                value={tournament.startDate}
                onChange={handleChange}
                InputLabelProps={{
                  shrink: true,
                }}
                error={Boolean(validationErrors.startDate)}
                helperText={validationErrors.startDate || ''}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                name="endDate"
                label="End Date"
                type="date"
                fullWidth
                value={tournament.endDate}
                onChange={handleChange}
                InputLabelProps={{
                  shrink: true,
                }}
                error={Boolean(validationErrors.endDate)}
                helperText={validationErrors.endDate || ''}
                required
              />
            </Grid>
            <Grid item xs={12} sm={4}>
              <TextField
                name="registrationDeadline"
                label="Registration Deadline"
                type="date"
                fullWidth
                value={tournament.registrationDeadline}
                onChange={handleChange}
                InputLabelProps={{
                  shrink: true,
                }}
                error={Boolean(validationErrors.registrationDeadline)}
                helperText={validationErrors.registrationDeadline || ''}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                name="maxParticipants"
                label="Maximum Participants"
                type="number"
                fullWidth
                value={tournament.maxParticipants}
                onChange={handleChange}
                inputProps={{ min: 2 }}
                error={Boolean(validationErrors.maxParticipants)}
                helperText={validationErrors.maxParticipants || ''}
                required
              />
            </Grid>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  variant="contained"
                  color="primary"
                  type="submit"
                  startIcon={<SaveIcon />}
                  disabled={saving}
                >
                  {saving ? 'Saving...' : 'Save Tournament'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      )}

      {/* Only show these tabs when editing an existing tournament */}
      {!isNewTournament && tabValue === 1 && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Player Registrations</Typography>
            </Box>
            
            {registrations.length === 0 ? (
              <Typography variant="body1" color="textSecondary">
                No registrations found for this tournament.
              </Typography>
            ) : (
              <List>
                {registrations.map((registration) => (
                  <React.Fragment key={registration.id}>
                    <ListItem sx={{ 
                      borderLeft: `4px solid ${getStatusColor(registration.status)}`,
                      backgroundColor: registration.status === 'PENDING' ? '#fff8e1' : 'transparent',
                      '&:hover': { backgroundColor: '#f5f5f5' }
                    }}>
                      <Grid container alignItems="center">
                        <Grid item xs={12} sm={4}>
                          <ListItemText
                            primary={registration.playerName}
                            secondary={`Registration Date: ${new Date(registration.registrationDate).toLocaleDateString()}`}
                          />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                          <Chip 
                            label={registration.status} 
                            color={getStatusColor(registration.status)}
                            size="small"
                            onClick={() => handleOpenStatusDialog(registration)}
                          />
                        </Grid>
                        <Grid item xs={12} sm={5}>
                          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
                            {registration.status === 'PENDING' && (
                              <>
                                <Button 
                                  size="small" 
                                  color="success"
                                  variant="outlined"
                                  startIcon={<ApproveIcon />}
                                  onClick={() => handleQuickApprove(registration)}
                                  sx={{ mr: 1 }}
                                >
                                  Approve
                                </Button>
                                <Button 
                                  size="small" 
                                  color="error"
                                  variant="outlined"
                                  startIcon={<RejectIcon />}
                                  onClick={() => handleQuickReject(registration)}
                                  sx={{ mr: 1 }}
                                >
                                  Reject
                                </Button>
                              </>
                            )}
                            <Button
                              size="small"
                              variant="outlined"
                              onClick={() => handleOpenStatusDialog(registration)}
                            >
                              Change Status
                            </Button>
                          </Box>
                        </Grid>
                      </Grid>
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      )}

      {!isNewTournament && tabValue === 2 && (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Typography variant="h6">Tournament Matches</Typography>
              <Button 
                variant="contained" 
                color="primary"
                startIcon={<AddIcon />}
                onClick={handleAddMatch}
              >
                Add Match
              </Button>
            </Box>
            
            {matches.length === 0 ? (
              <Typography variant="body1" color="textSecondary">
                No matches found for this tournament.
              </Typography>
            ) : (
              <List>
                {matches.map((match) => (
                  <React.Fragment key={match.id}>
                    <ListItem
                      button
                      onClick={() => navigate(`/admin/matches/${match.id}`)}
                      sx={{ 
                        borderLeft: `4px solid ${getMatchStatusColor(match.status)}`,
                        '&:hover': { backgroundColor: '#f5f5f5' }
                      }}
                    >
                      <Grid container alignItems="center">
                        <Grid item xs={12} sm={5}>
                          <ListItemText
                            primary={`${match.player1Name} vs ${match.player2Name}`}
                            secondary={`Referee: ${match.refereeName}`}
                          />
                        </Grid>
                        <Grid item xs={12} sm={4}>
                          <ListItemText 
                            secondary={`Court: ${match.courtNumber} - ${new Date(match.scheduledTime).toLocaleString()}`}
                          />
                        </Grid>
                        <Grid item xs={12} sm={3}>
                          <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                            <Chip 
                              label={match.status} 
                              color={getMatchStatusColor(match.status)}
                              size="small"
                            />
                          </Box>
                        </Grid>
                      </Grid>
                    </ListItem>
                    <Divider />
                  </React.Fragment>
                ))}
              </List>
            )}
          </CardContent>
        </Card>
      )}

      {/* Registration Status Update Dialog */}
      <Dialog 
        open={registrationDialogOpen} 
        onClose={handleCloseStatusDialog}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Update Registration Status</DialogTitle>
        <DialogContent>
          {selectedRegistration && (
            <>
              <Typography variant="subtitle1" gutterBottom>
                Player: {selectedRegistration.playerName}
              </Typography>
              <Typography variant="body2" gutterBottom>
                Current Status: <Chip 
                  label={selectedRegistration.status} 
                  color={getStatusColor(selectedRegistration.status)}
                  size="small"
                />
              </Typography>
              <Box sx={{ mt: 2 }}>
                <FormControl fullWidth>
                  <InputLabel>New Status</InputLabel>
                  <Select
                    value={newStatus}
                    label="New Status"
                    onChange={handleStatusChange}
                  >
                    <MenuItem value="PENDING">Pending</MenuItem>
                    <MenuItem value="APPROVED">Approved</MenuItem>
                    <MenuItem value="REJECTED">Rejected</MenuItem>
                    <MenuItem value="WAITLISTED">Waitlisted</MenuItem>
                  </Select>
                </FormControl>
              </Box>
            </>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseStatusDialog}>Cancel</Button>
          <Button 
            onClick={handleUpdateRegistrationStatus}
            variant="contained" 
            color="primary"
            disabled={statusSaving}
          >
            {statusSaving ? 'Updating...' : 'Update Status'}
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};

export default TournamentDetails;