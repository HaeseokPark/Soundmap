import { Client } from "@stomp/stompjs";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { notificationApi } from "../api/discovery";
import { chatApi } from "../api/chat";
import { tokenStorage } from "../api/client";
import { useAuth } from "../auth/AuthContext";

const ChatSocketContext = createContext(null);

/**
 * WebSocket 주소.
 *
 * 배포할 때는 nginx 가 프론트와 API 를 같은 도메인으로 서비스하므로
 * VITE_API_BASE_URL 을 비워 둔다. 그때는 현재 접속한 주소에서 유도해야 한다.
 * "/ws" 같은 상대 경로를 그대로 넘기면 STOMP 클라이언트가 거부한다.
 */
function resolveWebSocketUrl() {
  const base = import.meta.env.VITE_API_BASE_URL;
  if (base) {
    return `${base.replace(/^http/, "ws")}/ws`;
  }
  // https 로 열었으면 wss 로 붙어야 한다. ws 로 붙으면 브라우저가 막는다.
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws`;
}

const WS_URL = resolveWebSocketUrl();

/**
 * 앱 전체가 공유하는 WebSocket 연결.
 *
 * 화면마다 따로 연결하면 채팅방을 드나들 때마다 연결이 끊겼다 붙어서,
 * 그 사이에 온 메시지를 놓친다. 로그인해 있는 동안 하나만 유지한다.
 */
export function ChatSocketProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  // 방 목록 화면이 스스로 갱신하도록, 알림이 올 때마다 값을 올려 준다.
  const [notificationCount,setNotificationCount]=useState(0);
  const [notificationVersion,setNotificationVersion]=useState(0);
  const refreshNotifications=useCallback(()=>{
    notificationApi.inbox().then(data=>setNotificationCount(data.unreadCount)).catch(()=>{});
  },[]);
  useEffect(()=>{
    if(!isAuthenticated){setNotificationCount(0);return;}
    const pull=()=>{if(document.visibilityState==='visible')refreshNotifications();};
    pull();const timer=setInterval(pull,30000);document.addEventListener('visibilitychange',pull);
    return()=>{clearInterval(timer);document.removeEventListener('visibilitychange',pull);};
  },[isAuthenticated,refreshNotifications]);
  const [roomsVersion, setRoomsVersion] = useState(0);

  const refreshUnread = useCallback(() => {
    chatApi
      .unreadCount()
      .then(setUnreadCount)
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnreadCount(0);
      return undefined;
    }

    const token = tokenStorage.get();
    if (!token) return undefined;

    refreshUnread();

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      debug: () => {},
      onConnect: () => {
        setConnected(true);
        refreshNotifications();
        client.subscribe("/user/queue/notifications",()=>{setNotificationVersion(v=>v+1);refreshNotifications();});
        // 상대가 요청/수락/전송을 하면 이 큐로 신호가 온다.
        client.subscribe("/user/queue/chats", () => {
          setRoomsVersion((v) => v + 1);
          refreshUnread();
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      clientRef.current = null;
      setConnected(false);
      client.deactivate();
    };
  }, [isAuthenticated, refreshUnread, refreshNotifications]);

  /** 방 토픽 구독. 반환된 함수를 호출하면 구독이 해제된다. */
  const subscribeToRoom = useCallback((roomId, onMessage) => {
    const client = clientRef.current;
    if (!client || !client.connected) return () => {};

    const subscription = client.subscribe(`/topic/chats/${roomId}`, (frame) => {
      onMessage(JSON.parse(frame.body));
    });
    return () => subscription.unsubscribe();
  }, []);

  /**
   * WebSocket 으로 전송한다. 연결이 없으면 false 를 돌려주고,
   * 호출한 쪽이 REST 로 넘어가게 한다.
   */
  const sendOverSocket = useCallback((roomId, content) => {
    const client = clientRef.current;
    if (!client || !client.connected) return false;

    client.publish({
      destination: `/app/chats/${roomId}/send`,
      body: JSON.stringify({ content }),
    });
    return true;
  }, []);

  const value = useMemo(
    () => ({
      connected,
      notificationCount, notificationVersion, refreshNotifications,
      unreadCount,
      roomsVersion,
      refreshUnread,
      subscribeToRoom,
      sendOverSocket,
    }),
    [connected, unreadCount, roomsVersion, refreshUnread, subscribeToRoom, sendOverSocket, notificationCount, notificationVersion, refreshNotifications]
  );

  return <ChatSocketContext.Provider value={value}>{children}</ChatSocketContext.Provider>;
}

export function useChatSocket() {
  const context = useContext(ChatSocketContext);
  if (context === null) {
    throw new Error("useChatSocket 은 ChatSocketProvider 안에서만 쓸 수 있습니다.");
  }
  return context;
}
