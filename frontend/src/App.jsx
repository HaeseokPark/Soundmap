import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { ChatSocketProvider } from "./chat/ChatSocketContext";
import ProtectedRoute from "./components/ProtectedRoute";
import ChatListPage from "./pages/ChatListPage";
import ChatRoomPage from "./pages/ChatRoomPage";
import MapPage from "./pages/MapPage";
import OnboardingPage from "./pages/OnboardingPage";
import ProfilePage from "./pages/ProfilePage";
import SettingsPage from "./pages/SettingsPage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";

import { LibraryProvider } from './music/LibraryContext';
import MusicPage from './pages/MusicPage';
import NotificationsPage from './pages/NotificationsPage';

export default function App() {
  return (
    <AuthProvider>
      <LibraryProvider><ChatSocketProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/music/:section" element={<ProtectedRoute><MusicPage/></ProtectedRoute>}/>
            <Route path="/notifications" element={<ProtectedRoute><NotificationsPage/></ProtectedRoute>}/>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <MapPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/onboarding"
              element={
                <ProtectedRoute requireTaste={false}>
                  <OnboardingPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/users/:userId"
              element={
                <ProtectedRoute>
                  <ProfilePage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/chats"
              element={
                <ProtectedRoute>
                  <ChatListPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/chats/:roomId"
              element={
                <ProtectedRoute>
                  <ChatRoomPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/settings"
              element={
                <ProtectedRoute>
                  <SettingsPage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </ChatSocketProvider></LibraryProvider>
    </AuthProvider>
  );
}
