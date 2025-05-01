import React, { useState, useEffect, useCallback, useContext } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { 
  Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, 
  Button, Typography, Box, CircularProgress, Chip, Alert, TextField,
  InputAdornment, FormControl, InputLabel, Select, MenuItem, IconButton,
  Grid, Card, CardContent, Divider, Tooltip
} from '@mui/material';
import { 
  Search as SearchIcon, 
  Person as PersonIcon,
  FilterList as FilterIcon,
  Clear as ClearIcon,
  SportsHandball as HandIcon,
  EmojiEvents as TournamentIcon
} from '@mui/icons-material';
import { API_ENDPOINTS } from '../../config';
import { AuthContext } from '../../context/AuthContext';

const PlayerList = () => {
  const [players, setPlayers] = useState([]);
  const [filteredPlayers, setFilteredPlayers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState({
    handPreference: 'ALL',
    tournaments: 'ALL',
    tournamentStatus: 'ALL'
  });
  const [showFilters, setShowFilters] = useState(false);
  const [tournaments, setTournaments] = useState([]);
  
  const navigate = useNavigate();
  const { auth } = useContext(AuthContext);

  useEffect(() => {
    fetchPlayers();
    fetchTournaments();
  }, []);
  
  const fetchPlayers = async () => {
    setLoading(true);
    try {
      // Use the new Players API endpoint
      const res = await axios.get(API_ENDPOINTS.PLAYERS.GET_ALL);
      setPlayers(res.data);
      setFilteredPlayers(res.data);
      setError(null);
    } catch (err) {
      console.error('Error fetching players:', err);
      setError('Error fetching players. Please try again.');
      setFilteredPlayers([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchTournaments = async () => {
    try {
      const res = await axios.get(API_ENDPOINTS.TOURNAMENTS.GET_ALL);
      setTournaments(res.data);
    } catch (err) {
      console.error('Error fetching tournaments:', err);
    }
  };

  const handleSearchChange = (event) => {
    setSearchTerm(event.target.value);
  };

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters(prevFilters => ({
      ...prevFilters,
      [name]: value
    }));
  };
  
  const clearFilters = () => {
    setFilters({
      handPreference: 'ALL',
      tournaments: 'ALL',
      tournamentStatus: 'ALL'
    });
    setSearchTerm('');
    // Fetch all players again with no filters
    fetchPlayers();
  };

  const toggleFilters = () => {
    setShowFilters(!showFilters);
  };
  
  const applyFiltersAndSearch = useCallback(async () => {
    setLoading(true);
    setError(null);
    
    try {
      // Create filter DTO similar to the backend PlayerFilterDto
      const filterDto = {
        searchTerm: searchTerm.trim(),
        handPreference: filters.handPreference !== 'ALL' ? filters.handPreference : null,
        tournamentId: filters.tournaments !== 'ALL' ? parseInt(filters.tournaments) : null,
        tournamentStatus: filters.tournamentStatus !== 'ALL' ? filters.tournamentStatus : null
      };
      
      console.log('Applying filters:', filterDto);
      
      // If no actual filters are applied, just fetch all players
      if (!filterDto.searchTerm && !filterDto.handPreference && !filterDto.tournamentId) {
        await fetchPlayers();
        return;
      }
      
      // Use the new filter endpoint
      const response = await axios.post(API_ENDPOINTS.PLAYERS.FILTER, filterDto);
      setFilteredPlayers(response.data);
      setError(null);
      
      // Show a message if no results were found
      if (response.data.length === 0) {
        setError('No players found matching your criteria. Try adjusting your filters.');
      }
    } catch (err) {
      console.error('Error filtering players:', err);
      setError('Error applying filters. Please try again.');
      // Don't change the current list on error
    } finally {
      setLoading(false);
    }
  }, [players, filters, searchTerm]);

  const handleViewPlayerDetails = (playerId) => {
    // Navigate to player details page
    navigate(`/referee/players/${playerId}`);
  };
  
  // Helper function to get color based on tournament status
  const getTournamentStatusColor = (status) => {
    if (!status) return 'default';
    
    switch(status) {
      case 'APPROVED': return 'success';
      case 'PENDING': return 'warning';
      case 'WAITLISTED': return 'info';
      case 'REJECTED': return 'error';
      default: return 'default';
    }
  };

  return (
    <>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Player Directory
        </Typography>
        <Typography variant="body1" color="textSecondary">
          View and filter tennis players in the system.
        </Typography>
      </Box>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <TextField
              placeholder="Search players..."
              variant="outlined"
              size="small"
              fullWidth
              value={searchTerm}
              onChange={handleSearchChange}
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  applyFiltersAndSearch();
                }
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
                endAdornment: searchTerm && (
                  <InputAdornment position="end">
                    <IconButton size="small" onClick={() => {
                      setSearchTerm('');
                      // If we had active filters before, reapply them without the search term
                      if (filters.handPreference !== 'ALL' || filters.tournaments !== 'ALL') {
                        applyFiltersAndSearch();
                      } else {
                        // Otherwise just fetch all players
                        fetchPlayers();
                      }
                    }}>
                      <ClearIcon />
                    </IconButton>
                  </InputAdornment>
                )
              }}
              sx={{ maxWidth: 500, mr: 2 }}
            />
            <Box>
              <Button 
                variant="outlined" 
                startIcon={<FilterIcon />} 
                onClick={toggleFilters}
                color={showFilters ? "primary" : "inherit"}
              >
                {showFilters ? "Hide Filters" : "Show Filters"}
              </Button>
            </Box>
          </Box>

          {showFilters && (
            <>
              <Divider sx={{ my: 2 }} />
              <Grid container spacing={2} alignItems="center">
                {/* Hand Preference Filter */}
                <Grid item xs={12} sm={6} md={3}>
                  <FormControl fullWidth size="small">
                    <InputLabel id="hand-preference-label">Hand Preference</InputLabel>
                    <Select
                      labelId="hand-preference-label"
                      name="handPreference"
                      value={filters.handPreference}
                      label="Hand Preference"
                      onChange={handleFilterChange}
                      startIcon={<HandIcon />}
                      iconPosition="start"
                    >
                      <MenuItem value="ALL">All Players</MenuItem>
                      <MenuItem value="RIGHT">Right Handed</MenuItem>
                      <MenuItem value="LEFT">Left Handed</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                {/* Tournament Filter */}
                <Grid item xs={12} sm={6} md={3}>
                  <FormControl fullWidth size="small">
                    <InputLabel id="tournament-label">Tournament</InputLabel>
                    <Select
                      labelId="tournament-label"
                      name="tournaments"
                      value={filters.tournaments}
                      label="Tournament"
                      onChange={handleFilterChange}
                      startIcon={<TournamentIcon />}
                      iconPosition="start"
                    >
                      <MenuItem value="ALL">All Tournaments</MenuItem>
                      {tournaments.map(tournament => (
                        <MenuItem key={tournament.id} value={tournament.id}>
                          {tournament.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                
                {/* Tournament Status Filter */}
                <Grid item xs={12} sm={6} md={3}>
                  <FormControl 
                    fullWidth 
                    size="small" 
                    disabled={filters.tournaments === 'ALL'}
                  >
                    <InputLabel id="tournament-status-label">Registration Status</InputLabel>
                    <Select
                      labelId="tournament-status-label"
                      name="tournamentStatus"
                      value={filters.tournamentStatus || 'ALL'}
                      label="Registration Status"
                      onChange={handleFilterChange}
                    >
                      <MenuItem value="ALL">Any Status</MenuItem>
                      <MenuItem value="APPROVED">Approved</MenuItem>
                      <MenuItem value="PENDING">Pending</MenuItem>
                      <MenuItem value="WAITLISTED">Waitlisted</MenuItem>
                      <MenuItem value="REJECTED">Rejected</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item>
                  <Box sx={{ display: 'flex', gap: 1 }}>
                    <Button 
                      variant="contained" 
                      color="primary" 
                      size="small" 
                      onClick={applyFiltersAndSearch}
                      startIcon={<FilterIcon />}
                    >
                      Apply Filters
                    </Button>
                    <Button 
                      variant="outlined" 
                      color="secondary" 
                      size="small" 
                      onClick={clearFilters}
                      startIcon={<ClearIcon />}
                    >
                      Clear
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </>
          )}
        </CardContent>
      </Card>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Contact</TableCell>
              <TableCell>Hand Preference</TableCell>
              <TableCell>Tournament Status</TableCell>
              <TableCell align="center">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <CircularProgress size={24} />
                </TableCell>
              </TableRow>
            ) : filteredPlayers.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  No players found matching your criteria.
                </TableCell>
              </TableRow>
            ) : (
              filteredPlayers.map((player) => (
                <TableRow 
                  key={player.id}
                  sx={{ '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.04)' } }}
                >
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <PersonIcon sx={{ mr: 1, color: 'primary.main' }} />
                      <Box>
                        <Typography variant="body1">
                          {player.firstName} {player.lastName}
                        </Typography>
                        <Typography variant="caption" color="textSecondary">
                          @{player.username}
                        </Typography>
                      </Box>
                    </Box>
                  </TableCell>
                  <TableCell>
                    <Tooltip title="Email Address" placement="top">
                      <Typography variant="body2">{player.email}</Typography>
                    </Tooltip>
                  </TableCell>
                  <TableCell>
                    <Tooltip title="Dominant Hand" placement="top">
                      <Chip 
                        icon={<HandIcon fontSize="small" />}
                        size="small" 
                        label={player.handPreference || 'Not specified'} 
                        color={player.handPreference === 'LEFT' ? 'secondary' : 'primary'}
                        variant="outlined"
                      />
                    </Tooltip>
                  </TableCell>
                  <TableCell>
                    {player.tournamentStatus ? (
                      <Tooltip 
                        title={`Status in ${player.tournamentName || 'selected tournament'}`} 
                        placement="top"
                      >
                        <Chip 
                          icon={<TournamentIcon fontSize="small" />}
                          size="small" 
                          label={player.tournamentStatus || 'N/A'} 
                          color={getTournamentStatusColor(player.tournamentStatus)}
                          variant="outlined"
                        />
                      </Tooltip>
                    ) : (
                      <Typography variant="body2" color="textSecondary">
                        Not registered
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell align="center">
                    <Button
                      variant="contained"
                      size="small"
                      color="primary"
                      onClick={() => handleViewPlayerDetails(player.id)}
                    >
                      View Details
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};

export default PlayerList;