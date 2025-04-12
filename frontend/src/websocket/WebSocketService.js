import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

/**
 * Service to manage WebSocket connections for real-time communication
 */
class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.subscriptions = new Map();
  }

  /**
   * Connect to the WebSocket server
   * @param {string} token - JWT authentication token
   * @param {Function} onConnect - Callback function executed when connection is established
   * @param {Function} onError - Callback function executed when connection fails
   */
  connect(token, onConnect, onError) {
    if (this.connected) {
      if (onConnect) onConnect();
      return;
    }

    const socket = new SockJS('/ws');
    this.stompClient = Stomp.over(socket);
    
    // Disable debug logging in production
    this.stompClient.debug = null;

    this.stompClient.connect(
      { Authorization: `Bearer ${token}` },
      () => {
        this.connected = true;
        console.log('WebSocket connected');
        if (onConnect) onConnect();
      },
      (error) => {
        this.connected = false;
        console.error('WebSocket connection error:', error);
        if (onError) onError(error);
      }
    );
  }

  /**
   * Disconnect from the WebSocket server
   */
  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect();
      this.connected = false;
      this.subscriptions.clear();
      console.log('WebSocket disconnected');
    }
  }

  /**
   * Subscribe to a WebSocket topic
   * @param {string} topic - The topic to subscribe to
   * @param {Function} callback - Callback function executed when a message is received
   * @returns {string} Subscription ID
   */
  subscribe(topic, callback) {
    if (!this.connected || !this.stompClient) {
      console.error('Cannot subscribe: WebSocket not connected');
      return null;
    }

    const subscription = this.stompClient.subscribe(topic, (message) => {
      try {
        const payload = JSON.parse(message.body);
        callback(payload);
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
        callback(message.body);
      }
    });

    const subscriptionId = subscription.id;
    this.subscriptions.set(subscriptionId, subscription);
    
    console.log(`Subscribed to ${topic} with ID ${subscriptionId}`);
    return subscriptionId;
  }

  /**
   * Unsubscribe from a WebSocket topic
   * @param {string} subscriptionId - ID of the subscription to cancel
   */
  unsubscribe(subscriptionId) {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
      console.log(`Unsubscribed from subscription ${subscriptionId}`);
    }
  }

  /**
   * Send a message to a WebSocket destination
   * @param {string} destination - The destination to send the message to
   * @param {Object} body - The message body
   */
  send(destination, body) {
    if (!this.connected || !this.stompClient) {
      console.error('Cannot send message: WebSocket not connected');
      return;
    }

    this.stompClient.send(
      destination,
      {},
      typeof body === 'string' ? body : JSON.stringify(body)
    );
  }
}

// Create a singleton instance
const webSocketService = new WebSocketService();
export default webSocketService;