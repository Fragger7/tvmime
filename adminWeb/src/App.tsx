import React, { useState, useEffect } from 'react';
import { 
  Tv, 
  Plus, 
  Trash2, 
  CheckCircle2, 
  AlertCircle, 
  RefreshCw, 
  Server, 
  Shield, 
  Layers, 
  Power, 
  Copy, 
  Check, 
  Eye, 
  EyeOff, 
  Pencil, 
  Link as LinkIcon, 
  Film, 
  Video, 
  X,
  Sparkles,
  KeyRound,
  Download,
  Smartphone,
  ExternalLink,
  QrCode
} from 'lucide-react';

import { 
  auth, 
  onAuthStateChanged, 
  signInAnonymously, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signOut,
  updatePassword,
  updateProfile,
  reauthenticateWithCredential,
  EmailAuthProvider,
  subscribeToUserPortals, 
  addPortal, 
  updatePortal, 
  deletePortal,
  authorizeTvPairing,
  subscribeToStreamReports,
  type StreamIssueReport,
  type XtreamPortal, 
  type User 
} from './lib/firebase';
import { 
  testXtreamConnection, 
  parseM3uUrl, 
  generateMasterM3uUrl, 
  type XtreamAuthResult 
} from './lib/xtream';

function SlidingToggle({
  checked,
  onChange,
  label,
}: {
  checked: boolean;
  onChange: () => void;
  label?: string;
}) {
  return (
    <div 
      onClick={(e) => { e.preventDefault(); e.stopPropagation(); onChange(); }}
      className="inline-flex items-center gap-2 cursor-pointer select-none group"
      role="switch"
      aria-checked={checked}
    >
      <div 
        className={`relative inline-flex h-6 w-11 shrink-0 rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out ${
          checked ? 'bg-emerald-500 shadow-sm shadow-emerald-500/30' : 'bg-[#262632] group-hover:bg-[#343444]'
        }`}
      >
        <span
          className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-md ring-0 transition duration-200 ease-in-out ${
            checked ? 'translate-x-5' : 'translate-x-0'
          }`}
        />
      </div>
      {label && (
        <span className={`text-xs font-semibold transition-colors ${checked ? 'text-emerald-400' : 'text-gray-400'}`}>
          {label}
        </span>
      )}
    </div>
  );
}

export function App() {

  const [user, setUser] = useState<User | null>(null);
  const [loadingAuth, setLoadingAuth] = useState(true);
  const [portals, setPortals] = useState<XtreamPortal[]>([]);
  
  // Auth Form State
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [isRegistering, setIsRegistering] = useState(false);

  // Add / Edit Modal State
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editingPortalId, setEditingPortalId] = useState<string | null>(null);
  const [inputMode, setInputMode] = useState<'xtream' | 'm3u'>('xtream');
  
  // Form fields
  const [name, setName] = useState('');
  const [serverUrl, setServerUrl] = useState('');
  const [username, setUsername] = useState('');
  const [portalPassword, setPortalPassword] = useState('');
  const [m3uUrl, setM3uUrl] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [syncLive, setSyncLive] = useState(true);
  const [syncMovies, setSyncMovies] = useState(true);
  const [syncSeries, setSyncSeries] = useState(true);
  const [isActive, setIsActive] = useState(true);
  const [m3uDetectedNote, setM3uDetectedNote] = useState<string | null>(null);

  // Verification & Action states
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState<XtreamAuthResult | null>(null);
  const [saving, setSaving] = useState(false);

  // Credentials Modal State
  const [credsPortal, setCredsPortal] = useState<XtreamPortal | null>(null);
  const [credsShowPassword, setCredsShowPassword] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);

  // Account Settings Modal State
  const [isAccountModalOpen, setIsAccountModalOpen] = useState(false);
  const [accountDisplayName, setAccountDisplayName] = useState('');
  const [currentPasswordForChange, setCurrentPasswordForChange] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [accountLoading, setAccountLoading] = useState(false);
  const [accountMessage, setAccountMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // TV Pairing State
  const [isPairModalOpen, setIsPairModalOpen] = useState(
    window.location.pathname === '/pair' || new URLSearchParams(window.location.search).has('code')
  );
  const [pairCode, setPairCode] = useState(
    new URLSearchParams(window.location.search).get('code') || ''
  );
  const [pairLoading, setPairLoading] = useState(false);
  const [pairSuccess, setPairSuccess] = useState<string | null>(null);
  const [pairError, setPairError] = useState<string | null>(null);

  // Stream Reports State
  const [streamReports, setStreamReports] = useState<StreamIssueReport[]>([]);
  const [isReportsModalOpen, setIsReportsModalOpen] = useState(false);

  const handleAuthorizeTv = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!pairCode.trim()) {
      setPairError('Please enter a valid 6-character code from your TV screen.');
      return;
    }
    if (!user) {
      setPairError('Please sign in or use Quick Access first to link your TV.');
      return;
    }

    setPairLoading(true);
    setPairError(null);
    setPairSuccess(null);

    try {
      await authorizeTvPairing(pairCode, user.uid, user.email || user.displayName || 'TVMime Admin');
      setPairSuccess(`🎉 Android TV successfully authorized! Your TV will now download your ${portals.length} IPTV playlist(s).`);
    } catch (err: unknown) {
      console.error('Pairing error:', err);
      const msg = err instanceof Error ? err.message : String(err);
      setPairError(`Authorization failed: ${msg}`);
    } finally {
      setPairLoading(false);
    }
  };

  const handleUpdateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setAccountLoading(true);
    setAccountMessage(null);

    try {
      // 1. Update Display Name if changed
      if (accountDisplayName.trim() && accountDisplayName.trim() !== (user.displayName || '')) {
        await updateProfile(user, { displayName: accountDisplayName.trim() });
      }

      // 2. Update Password if entered
      if (newPassword) {
        if (newPassword.length < 6) {
          throw new Error('New password must be at least 6 characters long.');
        }
        if (newPassword !== confirmNewPassword) {
          throw new Error('New passwords do not match.');
        }

        // If user has email and provided current password, re-authenticate first
        if (currentPasswordForChange && user.email) {
          const credential = EmailAuthProvider.credential(user.email, currentPasswordForChange);
          await reauthenticateWithCredential(user, credential);
        }

        await updatePassword(user, newPassword);
      }

      setAccountMessage({ type: 'success', text: 'Account and password updated successfully!' });
      setCurrentPasswordForChange('');
      setNewPassword('');
      setConfirmNewPassword('');
    } catch (err: unknown) {
      console.error('Account update error:', err);
      let msg = err instanceof Error ? err.message : String(err);
      if (msg.includes('auth/requires-recent-login')) {
        msg = 'For security, please enter your current password to confirm this change.';
      } else if (msg.includes('auth/wrong-password') || msg.includes('auth/invalid-credential')) {
        msg = 'Current password is incorrect.';
      }
      setAccountMessage({ type: 'error', text: msg });
    } finally {
      setAccountLoading(false);
    }
  };

  // Listen to Auth
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (currentUser) => {
      setUser(currentUser);
      setLoadingAuth(false);
    });
    return () => unsub();
  }, []);

  // Listen to Firestore Portals for current user
  useEffect(() => {
    if (!user) {
      setPortals([]);
      return;
    }
    const unsub = subscribeToUserPortals(user.uid, (fetched) => {
      setPortals(fetched);
    });
    return () => unsub();
  }, [user]);

  // Listen to Stream Error Reports
  useEffect(() => {
    if (!user) {
      setStreamReports([]);
      return;
    }
    const unsub = subscribeToStreamReports((fetched) => {
      setStreamReports(fetched);
    });
    return () => unsub();
  }, [user]);

  const handleEmailAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    try {
      if (isRegistering) {
        await createUserWithEmailAndPassword(auth, email, password);
      } else {
        await signInWithEmailAndPassword(auth, email, password);
      }
    } catch (err: unknown) {
      setAuthError(err instanceof Error ? err.message : 'Authentication failed');
    }
  };

  const handleQuickAccess = async () => {
    setAuthError(null);
    try {
      await signInAnonymously(auth);
    } catch (err: unknown) {
      setAuthError(err instanceof Error ? err.message : 'Quick access failed');
    }
  };

  const openAddModal = () => {
    setEditingPortalId(null);
    setName('');
    setInputMode('xtream');
    setServerUrl('');
    setUsername('');
    setPortalPassword('');
    setM3uUrl('');
    setShowPassword(false);
    setSyncLive(true);
    setSyncMovies(true);
    setSyncSeries(true);
    setIsActive(true);
    setTestResult(null);
    setM3uDetectedNote(null);
    setIsFormModalOpen(true);
  };

  const openEditModal = (p: XtreamPortal) => {
    setEditingPortalId(p.id || null);
    setName(p.name);
    setInputMode(p.type === 'm3u' ? 'm3u' : 'xtream');
    setServerUrl(p.serverUrl || '');
    setUsername(p.username || '');
    setPortalPassword(p.password || '');
    setM3uUrl(p.m3uUrl || '');
    setShowPassword(false);
    setSyncLive(p.syncLive ?? true);
    setSyncMovies(p.syncMovies ?? true);
    setSyncSeries(p.syncSeries ?? true);
    setIsActive(p.isActive ?? true);
    setTestResult(null);
    setM3uDetectedNote(null);
    setIsFormModalOpen(true);
  };

  // M3U Link Change handler with automatic parameter extraction
  const handleM3uUrlChange = (value: string) => {
    setM3uUrl(value);
    setM3uDetectedNote(null);
    if (!value.trim()) return;

    const parsed = parseM3uUrl(value);
    if (parsed.isXtream && parsed.serverUrl && parsed.username && parsed.password) {
      setServerUrl(parsed.serverUrl);
      setUsername(parsed.username);
      setPortalPassword(parsed.password);
      setM3uDetectedNote(`Xtream credentials detected! Server: ${parsed.serverUrl}, User: ${parsed.username}`);
      if (!name) {
        setName(`${new URL(parsed.serverUrl).hostname} (Xtream)`);
      }
    }
  };

  const handleTestConnection = async () => {
    const urlToTest = serverUrl || m3uUrl;
    if (!urlToTest || !username || !portalPassword) {
      setTestResult({ success: false, message: 'Please provide Server URL, Username, and Password to test.' });
      return;
    }
    setTesting(true);
    setTestResult(null);
    const res = await testXtreamConnection(urlToTest, username, portalPassword);
    setTesting(false);
    setTestResult(res);
  };

  const handleSavePortal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    if (!name.trim()) return;

    setSaving(true);
    try {
      const isM3u = inputMode === 'm3u' && (!serverUrl || !username);
      const cleanServer = serverUrl ? serverUrl.trim().replace(/\/+$/, '') : '';
      
      const payload: Partial<XtreamPortal> = {
        userId: user.uid,
        name: name.trim(),
        serverUrl: cleanServer,
        username: username.trim(),
        password: portalPassword.trim(),
        type: isM3u ? 'm3u' : 'xtream',
        isActive: Boolean(isActive),
        syncLive: Boolean(syncLive),
        syncMovies: Boolean(syncMovies),
        syncSeries: Boolean(syncSeries),
      };

      if (m3uUrl && m3uUrl.trim()) {
        payload.m3uUrl = m3uUrl.trim();
      }

      if (testResult?.success) {
        payload.status = 'online';
      }
      if (testResult?.expDate) {
        payload.expiryDate = testResult.expDate;
      }

      if (editingPortalId) {
        await updatePortal(editingPortalId, payload);
      } else {
        await addPortal({
          ...payload,
          createdAt: Date.now(),
          status: testResult?.success ? 'online' : 'unknown',
        } as Omit<XtreamPortal, 'id'>);
      }

      setIsFormModalOpen(false);
    } catch (err: unknown) {
      console.error("Save portal error:", err);
      const msg = err instanceof Error ? err.message : String(err);
      alert(`Failed to save connection: ${msg}`);
    } finally {
      setSaving(false);
    }
  };


  const handleToggleActive = async (portal: XtreamPortal) => {
    if (!portal.id) return;
    await updatePortal(portal.id, { isActive: !portal.isActive });
  };

  const handleDelete = async (id: string, portalName: string) => {
    if (window.confirm(`Delete connection "${portalName}"?`)) {
      await deletePortal(id);
    }
  };

  const copyToClipboard = (text: string, fieldKey: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(fieldKey);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const openCredentialsModal = (portal: XtreamPortal) => {
    setCredsPortal(portal);
    setCredsShowPassword(false);
    setCopiedField(null);
  };

  if (loadingAuth) {
    return (
      <div className="min-h-screen bg-[#070709] flex items-center justify-center text-gray-400">
        <RefreshCw className="w-8 h-8 animate-spin text-[#e50914]" />
      </div>
    );
  }

  // Not Logged In View
  if (!user) {
    return (
      <div className="min-h-screen bg-[#070709] text-white flex flex-col justify-between p-4 sm:p-8 relative overflow-hidden">
        {/* Ambient Glows */}
        <div className="absolute top-1/4 left-1/3 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-[#e50914]/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-10 right-1/4 w-80 h-80 bg-blue-600/5 rounded-full blur-3xl pointer-events-none" />

        {/* Top Header */}
        <header className="max-w-6xl w-full mx-auto flex items-center justify-between py-4 relative z-10">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-[#e50914] rounded-xl text-white shadow-lg shadow-[#e50914]/30">
              <Tv className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-black text-xl tracking-wider text-white">TVMIME</span>
                <span className="text-[10px] bg-[#e50914]/20 text-[#ff1e27] border border-[#e50914]/40 px-2 py-0.5 rounded-full font-mono font-bold">v1.0.0</span>
              </div>
              <p className="text-[11px] text-gray-400">Zero-OOM IPTV Streaming Suite</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <a
              href="https://github.com/Fragger7/tvmime"
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-1.5 px-3.5 py-1.5 bg-[#121217] hover:bg-[#181822] border border-[#262632] hover:border-gray-600 text-gray-300 hover:text-white rounded-xl text-xs font-semibold transition"
            >
              <span>GitHub</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          </div>
        </header>

        {/* Hero & Content Split */}
        <main className="max-w-6xl w-full mx-auto my-auto py-8 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center relative z-10">
          
          {/* Left Side: App Downloads & Highlights (7 cols) */}
          <div className="lg:col-span-7 space-y-6">
            <div className="space-y-3">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#181822] border border-[#262632] text-xs text-gray-300">
                <Sparkles className="w-3.5 h-3.5 text-[#ff1e27]" />
                <span>Created by <strong className="text-white font-semibold">Faraz Ahmad</strong></span>
              </div>

              <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white leading-tight">
                The Fastest, Most Elegant <span className="text-[#ff1e27]">IPTV Player</span> for Android TV.
              </h1>

              <p className="text-sm sm:text-base text-gray-400 max-w-xl leading-relaxed">
                Stream 100,000+ channels with zero Out-Of-Memory crashes. Built with Kotlin Multiplatform, low-level token parsing, and hardware-accelerated Media3 decoding.
              </p>
            </div>

            {/* Direct Download Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
              {/* Android TV & Fire TV Card */}
              <div className="bg-[#121217] border border-[#262632] hover:border-[#ff1e27]/50 rounded-2xl p-5 space-y-4 transition flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="p-2 bg-[#e50914]/20 text-[#ff1e27] rounded-xl border border-[#e50914]/30">
                      <Tv className="w-5 h-5" />
                    </div>
                    <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950/50 border border-emerald-800/60 px-2 py-0.5 rounded-full">
                      TV Optimized
                    </span>
                  </div>
                  <h2 className="text-base font-bold text-white">Android TV / Fire TV</h2>
                  <p className="text-xs text-gray-400 mt-1">
                    D-Pad navigation, Live EPG timelines, and cinematic VOD grid.
                  </p>
                </div>

                <div className="space-y-2">
                  <a
                    href="/tv.apk"
                    download
                    className="w-full py-2.5 bg-[#e50914] hover:bg-[#ff1e27] text-white font-semibold rounded-xl text-xs transition shadow-lg shadow-[#e50914]/25 flex items-center justify-center gap-2"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download TV APK</span>
                  </a>

                  <div className="bg-[#181822] rounded-xl p-2.5 border border-[#262632] text-[11px] font-mono text-gray-300">
                    <p className="text-gray-500 font-sans text-[10px] font-semibold">Firestick Downloader App URL:</p>
                    <p className="text-[#ff1e27] font-bold select-all truncate mt-0.5">tvmime.vercel.app/tv.apk</p>
                  </div>
                </div>
              </div>

              {/* Android Mobile Card */}
              <div className="bg-[#121217] border border-[#262632] hover:border-[#383848] rounded-2xl p-5 space-y-4 transition flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className="p-2 bg-blue-500/10 text-blue-400 rounded-xl border border-blue-500/20">
                      <Smartphone className="w-5 h-5" />
                    </div>
                    <span className="text-[10px] font-mono text-blue-400 bg-blue-950/50 border border-blue-800/60 px-2 py-0.5 rounded-full">
                      Mobile & Tablet
                    </span>
                  </div>
                  <h2 className="text-base font-bold text-white">Android Mobile</h2>
                  <p className="text-xs text-gray-400 mt-1">
                    Touch interface with fast channel search and portrait/landscape playback.
                  </p>
                </div>

                <div className="space-y-2">
                  <a
                    href="/mobile.apk"
                    download
                    className="w-full py-2.5 bg-[#181822] hover:bg-[#222230] border border-[#262632] hover:border-gray-600 text-white font-semibold rounded-xl text-xs transition flex items-center justify-center gap-2"
                  >
                    <Download className="w-4 h-4" />
                    <span>Download Mobile APK</span>
                  </a>

                  <div className="bg-[#181822] rounded-xl p-2.5 border border-[#262632] text-[11px] font-mono text-gray-300">
                    <p className="text-gray-500 font-sans text-[10px] font-semibold">Direct Browser Download URL:</p>
                    <p className="text-blue-400 font-bold select-all truncate mt-0.5">tvmime.vercel.app/mobile.apk</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Side: Cloud Portal Auth (5 cols) */}
          <div className="lg:col-span-5">
            <div className="w-full bg-[#121217] border border-[#262632] rounded-2xl p-6 sm:p-8 shadow-2xl space-y-5">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <Shield className="w-4 h-4 text-[#e50914]" />
                  <h2 className="text-lg font-bold text-white tracking-wide">Admin Cloud Vault</h2>
                </div>
                <p className="text-xs text-gray-400">
                  Manage Xtream playlists in the cloud. Syncs instantly to all your TV and mobile players.
                </p>
              </div>

              {authError && (
                <div className="p-3 bg-red-950/50 border border-red-800/80 rounded-xl text-xs text-red-200 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 text-[#e50914]" />
                  <span>{authError}</span>
                </div>
              )}

              <form onSubmit={handleEmailAuth} className="space-y-3.5">
                <div>
                  <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">Email</label>
                  <input 
                    type="email" 
                    value={email} 
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="admin@tvmime.com"
                    required
                    className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-gray-500 transition-colors"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">Password</label>
                  <input 
                    type="password" 
                    value={password} 
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-gray-500 transition-colors"
                  />
                </div>

                <button 
                  type="submit"
                  className="w-full py-2.5 bg-[#e50914] hover:bg-[#ff1e27] text-white font-semibold rounded-xl text-xs transition shadow-lg shadow-[#e50914]/25 cursor-pointer"
                >
                  {isRegistering ? 'Create Admin Account' : 'Sign In to Portal'}
                </button>
              </form>

              <div className="relative my-4 text-center">
                <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-[#262632]"></div></div>
                <span className="relative bg-[#121217] px-3 text-[11px] text-gray-500 uppercase tracking-wider">Or</span>
              </div>

              <button 
                type="button"
                onClick={handleQuickAccess}
                className="w-full py-2.5 bg-[#181822] hover:bg-[#20202c] border border-[#262632] text-gray-300 hover:text-white font-medium rounded-xl text-xs transition flex items-center justify-center gap-2 cursor-pointer"
              >
                <Shield className="w-4 h-4 text-emerald-400" />
                <span>Instant Anonymous Access</span>
              </button>

              <div className="text-center pt-1">
                <button
                  type="button"
                  onClick={() => setIsRegistering(!isRegistering)}
                  className="text-xs text-gray-400 hover:text-[#ff1e27] transition cursor-pointer"
                >
                  {isRegistering ? 'Already have an account? Sign In' : 'Need an account? Register with Email'}
                </button>
              </div>
            </div>
          </div>
        </main>

        {/* Footer */}
        <footer className="max-w-6xl w-full mx-auto text-center py-4 text-xs text-gray-500 relative z-10 border-t border-[#262632]/50">
          <p>© 2026 TVMime • Creator: Faraz Ahmad • Open Source</p>
        </footer>
      </div>
    );
  }

  // Logged In Dashboard View
  const activeCount = portals.filter(p => p.isActive).length;

  return (
    <div className="min-h-screen bg-[#070709] text-gray-100 flex flex-col">
      {/* Top Header */}
      <header className="border-b border-[#262632] bg-[#0c0c10]/80 backdrop-blur-md sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-[#e50914] rounded-lg text-white shadow-md shadow-[#e50914]/30">
              <Tv className="w-5 h-5" />
            </div>
            <div>
              <span className="font-extrabold text-lg tracking-wider text-white">TVMIME</span>
              <span className="ml-2 text-xs bg-[#e50914]/20 text-[#ff1e27] border border-[#e50914]/40 px-2 py-0.5 rounded-full font-mono font-medium">ADMIN</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <p className="text-xs text-gray-400">Authenticated User</p>
              <p className="text-xs font-mono text-gray-200 truncate max-w-[200px]">
                {user.displayName ? `${user.displayName} (${user.email || user.uid.slice(0, 6)})` : user.email || `Anon (${user.uid.slice(0, 8)})`}
              </p>
            </div>

            <a
              href="/tv.apk"
              download
              title="Download TV APK (tv.apk)"
              className="flex items-center gap-1.5 px-3 py-2 bg-[#181822] hover:bg-[#20202c] border border-[#262632] hover:border-[#ff1e27]/60 text-gray-300 hover:text-white rounded-xl text-xs font-semibold transition"
            >
              <Download className="w-4 h-4 text-[#ff1e27]" />
              <span className="hidden md:inline">Download TV APK</span>
            </a>

            <button 
              onClick={() => {
                setAccountDisplayName(user.displayName || '');
                setCurrentPasswordForChange('');
                setNewPassword('');
                setConfirmNewPassword('');
                setAccountMessage(null);
                setIsAccountModalOpen(true);
              }}
              title="Account & Password Settings"
              className="flex items-center gap-1.5 px-3 py-2 bg-[#181822] hover:bg-[#20202c] border border-[#262632] hover:border-[#ff1e27]/60 text-gray-300 hover:text-white rounded-xl text-xs font-semibold transition cursor-pointer"
            >
              <KeyRound className="w-4 h-4 text-emerald-400" />
              <span className="hidden md:inline">Account & Password</span>
            </button>

            <button
              onClick={() => {
                setPairCode('');
                setPairError(null);
                setPairSuccess(null);
                setIsPairModalOpen(true);
              }}
              title="Pair Android TV Device"
              className="flex items-center gap-1.5 px-3 py-2 bg-[#181822] hover:bg-[#20202c] border border-[#262632] hover:border-blue-500/60 text-gray-300 hover:text-white rounded-xl text-xs font-semibold transition cursor-pointer"
            >
              <QrCode className="w-4 h-4 text-blue-400" />
              <span className="hidden lg:inline">Pair Android TV</span>
            </button>

            {streamReports.length > 0 && (
              <button
                onClick={() => setIsReportsModalOpen(true)}
                title="View Stream Issue Reports"
                className="flex items-center gap-1.5 px-3 py-2 bg-red-950/40 hover:bg-red-950/70 border border-red-800/60 text-red-200 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                <AlertCircle className="w-4 h-4 text-[#ff1e27]" />
                <span className="hidden lg:inline">{streamReports.length} Issue{streamReports.length > 1 ? 's' : ''}</span>
              </button>
            )}

            <button 
              onClick={() => signOut(auth)}
              title="Sign Out"
              className="p-2 hover:bg-[#181822] text-gray-400 hover:text-white rounded-xl transition border border-transparent hover:border-[#262632] cursor-pointer"
            >
              <Power className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Welcome & Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-[#121217] border border-[#262632] rounded-2xl p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Total Connections</p>
                <p className="text-2xl font-bold text-white mt-1">{portals.length}</p>
              </div>
              <div className="p-3 bg-[#181822] rounded-xl text-gray-400">
                <Server className="w-6 h-6" />
              </div>
            </div>
          </div>

          <div className="bg-[#121217] border border-[#262632] rounded-2xl p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Active for TV Player</p>
                <p className="text-2xl font-bold text-[#ff1e27] mt-1">{activeCount}</p>
              </div>
              <div className="p-3 bg-[#e50914]/10 rounded-xl text-[#e50914]">
                <Layers className="w-6 h-6" />
              </div>
            </div>
          </div>

          <div className="bg-[#121217] border border-[#262632] rounded-2xl p-5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-gray-400 font-medium uppercase tracking-wider">Firestore Cloud State</p>
                <p className="text-sm font-semibold text-emerald-400 mt-1 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  Connected & Live
                </p>
              </div>
              <div className="p-3 bg-emerald-950/40 rounded-xl text-emerald-400">
                <CheckCircle2 className="w-6 h-6" />
              </div>
            </div>
          </div>
        </div>

        {/* Action Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-bold text-white tracking-wide">IPTV Connections & Playlists</h2>
            <p className="text-xs text-gray-400 mt-0.5">
              These credentials sync automatically to your TV player. Toggle items on/off or filter content types.
            </p>
          </div>

          <button
            onClick={openAddModal}
            className="flex items-center justify-center gap-2 px-4 py-2.5 bg-[#e50914] hover:bg-[#ff1e27] text-white font-semibold rounded-xl text-sm transition shadow-lg shadow-[#e50914]/25 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add Connection / M3U</span>
          </button>
        </div>

        {/* Portal Cards Grid */}
        {portals.length === 0 ? (
          <div className="border border-dashed border-[#262632] rounded-2xl p-12 text-center bg-[#121217]/50">
            <Server className="w-12 h-12 text-gray-600 mx-auto mb-3" />
            <h3 className="text-base font-semibold text-gray-300">No Connections Added Yet</h3>
            <p className="text-xs text-gray-500 max-w-sm mx-auto mt-1 mb-6">
              Add your first Xtream Codes provider or paste a full M3U playlist link to sync directly to your TV.
            </p>
            <button
              onClick={openAddModal}
              className="inline-flex items-center gap-2 px-4 py-2 bg-[#e50914] hover:bg-[#ff1e27] text-white text-xs font-semibold rounded-lg transition cursor-pointer"
            >
              <Plus className="w-4 h-4" /> Add Connection
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {portals.map((p) => {
              const isEnabled = p.isActive !== false;
              const liveOn = p.syncLive !== false;
              const moviesOn = p.syncMovies !== false;
              const seriesOn = p.syncSeries !== false;

              return (
                <div 
                  key={p.id} 
                  className={`bg-[#121217] border ${isEnabled ? 'border-[#262632]' : 'border-gray-800 opacity-60'} rounded-2xl p-5 flex flex-col justify-between transition-all hover:border-[#383848]`}
                >
                  <div>
                    {/* Card Top: Status & Name */}
                    <div className="flex items-start justify-between gap-3 mb-3">
                      <div className="flex items-center gap-2.5">
                        <div className={`w-3 h-3 rounded-full ${isEnabled ? 'bg-emerald-400' : 'bg-gray-600'}`}></div>
                        <h3 className="font-bold text-base text-white">{p.name}</h3>
                        <span className="text-[10px] uppercase font-mono px-2 py-0.5 rounded bg-[#181822] text-gray-400 border border-[#262632]">
                          {p.type || 'xtream'}
                        </span>
                      </div>

                      {/* Top Right: Enable/Disable Switch & Actions */}
                      <div className="flex items-center gap-3">
                        {/* Enable/Disable Sliding Toggle */}
                        <SlidingToggle
                          checked={isEnabled}
                          onChange={() => handleToggleActive(p)}
                          label={isEnabled ? "Active" : "Disabled"}
                        />


                        {/* Edit Button */}
                        <button
                          onClick={() => openEditModal(p)}
                          className="p-1.5 text-gray-400 hover:text-white hover:bg-[#181822] rounded-lg transition cursor-pointer"
                          title="Edit Connection Details"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>

                        {/* Delete Button */}
                        <button
                          onClick={() => p.id && handleDelete(p.id, p.name)}
                          className="p-1.5 text-gray-500 hover:text-red-400 hover:bg-[#181822] rounded-lg transition cursor-pointer"
                          title="Delete Connection"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {/* Details Box */}
                    <div className="bg-[#181822] rounded-xl p-3 space-y-2 border border-[#262632]/80 text-xs font-mono">
                      {p.serverUrl ? (
                        <div className="flex items-center justify-between text-gray-400">
                          <span className="text-gray-500">Host:</span>
                          <span className="text-gray-200 truncate max-w-[280px]">{p.serverUrl}</span>
                        </div>
                      ) : p.m3uUrl ? (
                        <div className="flex items-center justify-between text-gray-400">
                          <span className="text-gray-500">M3U:</span>
                          <span className="text-gray-200 truncate max-w-[280px]">{p.m3uUrl}</span>
                        </div>
                      ) : null}

                      {p.username && (
                        <div className="flex items-center justify-between text-gray-400">
                          <span className="text-gray-500">User:</span>
                          <span className="text-gray-200">{p.username}</span>
                        </div>
                      )}

                      {p.expiryDate && (
                        <div className="flex items-center justify-between text-gray-400">
                          <span className="text-gray-500">Expiry:</span>
                          <span className="text-amber-400">{p.expiryDate}</span>
                        </div>
                      )}

                      {/* Content Sync Filter Badges */}
                      <div className="pt-2 mt-2 border-t border-[#262632] flex items-center justify-between">
                        <span className="text-gray-500 text-[11px] font-sans">Content Filter:</span>
                        <div className="flex items-center gap-1.5 font-sans">
                          <span className={`text-[10px] px-2 py-0.5 rounded font-semibold border ${
                            liveOn 
                              ? 'bg-red-950/60 text-red-300 border-red-800' 
                              : 'bg-[#121217] text-gray-600 border-gray-800 line-through'
                          }`}>
                            Live
                          </span>
                          <span className={`text-[10px] px-2 py-0.5 rounded font-semibold border ${
                            moviesOn 
                              ? 'bg-blue-950/60 text-blue-300 border-blue-800' 
                              : 'bg-[#121217] text-gray-600 border-gray-800 line-through'
                          }`}>
                            Movies
                          </span>
                          <span className={`text-[10px] px-2 py-0.5 rounded font-semibold border ${
                            seriesOn 
                              ? 'bg-purple-950/60 text-purple-300 border-purple-800' 
                              : 'bg-[#121217] text-gray-600 border-gray-800 line-through'
                          }`}>
                            Series
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Card Bottom: Timestamp & Credentials Modal Trigger */}
                  <div className="mt-4 pt-3 border-t border-[#262632] flex items-center justify-between text-xs">
                    <span className="text-gray-500">
                      Added {new Date(p.createdAt).toLocaleDateString()}
                    </span>

                    <button
                      onClick={() => openCredentialsModal(p)}
                      className="flex items-center gap-1.5 px-3 py-1.5 bg-[#181822] hover:bg-[#222230] border border-[#262632] rounded-lg text-gray-300 hover:text-white transition cursor-pointer"
                    >
                      <Copy className="w-3.5 h-3.5 text-[#e50914]" />
                      <span>View & Copy Credentials</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Pairing Information Card */}
        <div className="bg-gradient-to-r from-[#121217] to-[#181822] border border-[#262632] rounded-2xl p-6">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-[#e50914]/20 border border-[#e50914]/30 rounded-xl text-[#ff1e27] shrink-0">
              <Tv className="w-6 h-6" />
            </div>
            <div>
              <h3 className="font-bold text-white text-base">Android TV Sync Engine</h3>
              <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                When TVMime launches on your TV or Firestick, it connects to your Firestore vault and streams these connections directly 
                into its fast local SQLite database. Playlists marked <span className="text-emerald-400 font-semibold">Enabled</span> will sync, 
                while <span className="text-gray-400 font-semibold">Disabled</span> ones are ignored. Content filters (Live/Movies/Series) prevent 
                memory bloat on low-RAM devices!
              </p>
            </div>
          </div>
        </div>
      </main>

      {/* ============================================================ */}
      {/* ADD / EDIT CONNECTION MODAL */}
      {/* ============================================================ */}
      {isFormModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm overflow-y-auto">
          <div className="relative w-full max-w-lg bg-[#121217] border border-[#262632] rounded-2xl p-6 shadow-2xl space-y-5 my-8">
            <div className="flex items-center justify-between border-b border-[#262632] pb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-[#e50914] rounded-lg text-white">
                  <Server className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">
                    {editingPortalId ? 'Edit Connection' : 'Add New Connection'}
                  </h3>
                  <p className="text-xs text-gray-400">Configure parameters or paste an M3U link</p>
                </div>
              </div>
              <button 
                onClick={() => setIsFormModalOpen(false)}
                className="text-gray-400 hover:text-white p-1 rounded-lg transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Input Mode Switcher (Xtream Parameters vs Full M3U) */}
            <div className="grid grid-cols-2 gap-2 bg-[#181822] p-1 rounded-xl border border-[#262632]">
              <button
                type="button"
                onClick={() => setInputMode('xtream')}
                className={`py-2 text-xs font-semibold rounded-lg transition cursor-pointer ${
                  inputMode === 'xtream' 
                    ? 'bg-[#e50914] text-white shadow' 
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                Xtream Parameters
              </button>
              <button
                type="button"
                onClick={() => setInputMode('m3u')}
                className={`py-2 text-xs font-semibold rounded-lg transition cursor-pointer flex items-center justify-center gap-1.5 ${
                  inputMode === 'm3u' 
                    ? 'bg-[#e50914] text-white shadow' 
                    : 'text-gray-400 hover:text-white'
                }`}
              >
                <LinkIcon className="w-3.5 h-3.5" />
                <span>Full M3U Link</span>
              </button>
            </div>

            <form onSubmit={handleSavePortal} className="space-y-4">
              {/* Connection Name */}
              <div>
                <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                  Connection Name / Nickname
                </label>
                <input 
                  type="text" 
                  value={name} 
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Primary 4K Workstation"
                  required
                  className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-500"
                />
              </div>

              {/* Mode: Full M3U Link */}
              {inputMode === 'm3u' && (
                <div className="space-y-3">
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                      M3U / M3U_PLUS Playlist URL
                    </label>
                    <textarea 
                      value={m3uUrl} 
                      onChange={(e) => handleM3uUrlChange(e.target.value)}
                      placeholder="http://provider-domain.com:8080/get.php?username=XXX&password=YYY&type=m3u_plus&output=ts"
                      rows={3}
                      className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl px-4 py-2.5 text-xs text-white placeholder-gray-500 font-mono"
                    />
                  </div>

                  {m3uDetectedNote && (
                    <div className="p-3 bg-emerald-950/40 border border-emerald-800/80 rounded-xl text-xs text-emerald-200 flex items-start gap-2">
                      <Sparkles className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span>{m3uDetectedNote}</span>
                    </div>
                  )}
                </div>
              )}

              {/* Mode: Xtream Parameters */}
              {(inputMode === 'xtream' || (inputMode === 'm3u' && serverUrl)) && (
                <div className="space-y-3 pt-1">
                  <div>
                    <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                      Server URL
                    </label>
                    <input 
                      type="text" 
                      value={serverUrl} 
                      onChange={(e) => setServerUrl(e.target.value)}
                      placeholder="http://provider-dns.me:8080"
                      required={inputMode === 'xtream'}
                      className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-500 font-mono"
                    />
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                        Username
                      </label>
                      <input 
                        type="text" 
                        value={username} 
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="xtream_user"
                        required={inputMode === 'xtream'}
                        className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl px-4 py-2.5 text-sm text-white placeholder-gray-500 font-mono"
                      />
                    </div>

                    {/* Password with Reveal/Hide Toggle */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-1">
                        Password
                      </label>
                      <div className="relative">
                        <input 
                          type={showPassword ? "text" : "password"} 
                          value={portalPassword} 
                          onChange={(e) => setPortalPassword(e.target.value)}
                          placeholder="••••••••"
                          required={inputMode === 'xtream'}
                          className="w-full bg-[#181822] border border-[#262632] focus:border-[#e50914] focus:outline-none rounded-xl pl-4 pr-10 py-2.5 text-sm text-white placeholder-gray-500 font-mono"
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white transition cursor-pointer"
                          title={showPassword ? "Hide password" : "Show password"}
                        >
                          {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Content Type Sync Toggles (Live / Movies / Series) */}
              <div className="pt-2 border-t border-[#262632]">
                <label className="block text-xs font-semibold text-gray-300 uppercase tracking-wider mb-2">
                  Content Sync Toggles (Download Filter)
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {/* Live Toggle */}
                  <button
                    type="button"
                    onClick={() => setSyncLive(!syncLive)}
                    className={`p-2.5 rounded-xl border text-xs font-medium flex flex-col items-center gap-1.5 transition cursor-pointer ${
                      syncLive 
                        ? 'bg-red-950/40 border-red-700/80 text-white shadow-sm' 
                        : 'bg-[#181822] border-[#262632] text-gray-500'
                    }`}
                  >
                    <Tv className={`w-4 h-4 ${syncLive ? 'text-[#ff1e27]' : 'text-gray-500'}`} />
                    <span>Live Channels</span>
                    <span className={`text-[10px] font-bold ${syncLive ? 'text-emerald-400' : 'text-gray-500'}`}>
                      {syncLive ? 'ON' : 'OFF'}
                    </span>
                  </button>

                  {/* Movies Toggle */}
                  <button
                    type="button"
                    onClick={() => setSyncMovies(!syncMovies)}
                    className={`p-2.5 rounded-xl border text-xs font-medium flex flex-col items-center gap-1.5 transition cursor-pointer ${
                      syncMovies 
                        ? 'bg-blue-950/40 border-blue-700/80 text-white shadow-sm' 
                        : 'bg-[#181822] border-[#262632] text-gray-500'
                    }`}
                  >
                    <Film className={`w-4 h-4 ${syncMovies ? 'text-blue-400' : 'text-gray-500'}`} />
                    <span>VOD Movies</span>
                    <span className={`text-[10px] font-bold ${syncMovies ? 'text-emerald-400' : 'text-gray-500'}`}>
                      {syncMovies ? 'ON' : 'OFF'}
                    </span>
                  </button>

                  {/* Series Toggle */}
                  <button
                    type="button"
                    onClick={() => setSyncSeries(!syncSeries)}
                    className={`p-2.5 rounded-xl border text-xs font-medium flex flex-col items-center gap-1.5 transition cursor-pointer ${
                      syncSeries 
                        ? 'bg-purple-950/40 border-purple-700/80 text-white shadow-sm' 
                        : 'bg-[#181822] border-[#262632] text-gray-500'
                    }`}
                  >
                    <Video className={`w-4 h-4 ${syncSeries ? 'text-purple-400' : 'text-gray-500'}`} />
                    <span>TV Series</span>
                    <span className={`text-[10px] font-bold ${syncSeries ? 'text-emerald-400' : 'text-gray-500'}`}>
                      {syncSeries ? 'ON' : 'OFF'}
                    </span>
                  </button>
                </div>
              </div>

              {/* Master Playlist Enabled Switch */}
              <div className="pt-2 border-t border-[#262632] flex items-center justify-between">
                <div>
                  <p className="text-xs font-semibold text-white">Enable Connection for Player</p>
                  <p className="text-[11px] text-gray-400">If disabled, player app will ignore and not download this playlist</p>
                </div>
                <SlidingToggle
                  checked={isActive}
                  onChange={() => setIsActive(!isActive)}
                  label={isActive ? "Active" : "Disabled"}
                />
              </div>


              {/* Test Result Feedback */}
              {testResult && (
                <div className={`p-3 rounded-xl border text-xs flex items-start gap-2.5 ${
                  testResult.success 
                    ? 'bg-emerald-950/40 border-emerald-800 text-emerald-200' 
                    : 'bg-amber-950/40 border-amber-800 text-amber-200'
                }`}>
                  {testResult.success ? (
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  ) : (
                    <AlertCircle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                  )}
                  <div>
                    <p className="font-semibold">{testResult.message}</p>
                    {testResult.expDate && (
                      <p className="text-[11px] text-gray-400 mt-1">
                        Expiry: <span className="text-white">{testResult.expDate}</span> | Connections: {testResult.activeCons}/{testResult.maxCons}
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* Modal Buttons */}
              <div className="flex items-center justify-between pt-3 border-t border-[#262632] gap-3">
                <button
                  type="button"
                  onClick={handleTestConnection}
                  disabled={testing}
                  className="px-4 py-2.5 bg-[#181822] hover:bg-[#20202c] border border-[#262632] text-gray-300 hover:text-white rounded-xl text-xs font-semibold flex items-center gap-2 transition cursor-pointer"
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${testing ? 'animate-spin text-[#e50914]' : ''}`} />
                  <span>{testing ? 'Testing...' : 'Test Connection'}</span>
                </button>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setIsFormModalOpen(false)}
                    className="px-4 py-2.5 text-gray-400 hover:text-white text-xs font-semibold rounded-xl transition cursor-pointer"
                  >
                    Cancel
                  </button>

                  <button
                    type="submit"
                    disabled={saving}
                    className="px-5 py-2.5 bg-[#e50914] hover:bg-[#ff1e27] text-white text-xs font-semibold rounded-xl transition shadow-lg shadow-[#e50914]/25 cursor-pointer"
                  >
                    {saving ? 'Saving...' : editingPortalId ? 'Save Changes' : 'Add Connection'}
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ============================================================ */}
      {/* CREDENTIALS & QUICK COPY MODAL */}
      {/* ============================================================ */}
      {credsPortal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="relative w-full max-w-lg bg-[#121217] border border-[#262632] rounded-2xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-[#262632] pb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-[#e50914] rounded-lg text-white">
                  <Copy className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">{credsPortal.name}</h3>
                  <p className="text-xs text-gray-400">Connection Credentials & Master M3U Link</p>
                </div>
              </div>
              <button 
                onClick={() => setCredsPortal(null)}
                className="text-gray-400 hover:text-white p-1 rounded-lg transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs font-mono">
              {/* Server URL */}
              {credsPortal.serverUrl && (
                <div>
                  <label className="block text-[11px] font-sans font-semibold text-gray-400 mb-1">Server URL</label>
                  <div className="flex items-center gap-2 bg-[#181822] border border-[#262632] rounded-xl px-3 py-2">
                    <span className="text-gray-200 truncate flex-1">{credsPortal.serverUrl}</span>
                    <button
                      onClick={() => copyToClipboard(credsPortal.serverUrl, 'server')}
                      className="p-1.5 bg-[#121217] hover:bg-[#222230] border border-[#262632] text-gray-300 hover:text-white rounded-lg transition cursor-pointer"
                      title="Copy Server URL"
                    >
                      {copiedField === 'server' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              )}

              {/* Username */}
              {credsPortal.username && (
                <div>
                  <label className="block text-[11px] font-sans font-semibold text-gray-400 mb-1">Username</label>
                  <div className="flex items-center gap-2 bg-[#181822] border border-[#262632] rounded-xl px-3 py-2">
                    <span className="text-gray-200 flex-1">{credsPortal.username}</span>
                    <button
                      onClick={() => copyToClipboard(credsPortal.username, 'user')}
                      className="p-1.5 bg-[#121217] hover:bg-[#222230] border border-[#262632] text-gray-300 hover:text-white rounded-lg transition cursor-pointer"
                      title="Copy Username"
                    >
                      {copiedField === 'user' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              )}

              {/* Password with Reveal and Copy */}
              {credsPortal.password && (
                <div>
                  <label className="block text-[11px] font-sans font-semibold text-gray-400 mb-1">Password</label>
                  <div className="flex items-center gap-2 bg-[#181822] border border-[#262632] rounded-xl px-3 py-2">
                    <span className="text-gray-200 flex-1">
                      {credsShowPassword ? credsPortal.password : '••••••••••••'}
                    </span>
                    <button
                      type="button"
                      onClick={() => setCredsShowPassword(!credsShowPassword)}
                      className="p-1.5 text-gray-400 hover:text-white transition cursor-pointer"
                      title={credsShowPassword ? "Hide password" : "Show password"}
                    >
                      {credsShowPassword ? <EyeOff className="w-3.5 h-3.5" /> : <Eye className="w-3.5 h-3.5" />}
                    </button>
                    <button
                      onClick={() => copyToClipboard(credsPortal.password, 'pass')}
                      className="p-1.5 bg-[#121217] hover:bg-[#222230] border border-[#262632] text-gray-300 hover:text-white rounded-lg transition cursor-pointer"
                      title="Copy Password"
                    >
                      {copiedField === 'pass' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              )}

              {/* Master M3U Plus Link */}
              {credsPortal.serverUrl && credsPortal.username && credsPortal.password && (
                <div>
                  <label className="block text-[11px] font-sans font-semibold text-gray-400 mb-1">
                    Master M3U Plus Link (Quick-Add for any IPTV app)
                  </label>
                  <div className="flex items-center gap-2 bg-[#181822] border border-[#262632] rounded-xl px-3 py-2">
                    <span className="text-gray-200 truncate flex-1 text-[11px]">
                      {generateMasterM3uUrl(credsPortal.serverUrl, credsPortal.username, credsPortal.password)}
                    </span>
                    <button
                      onClick={() => copyToClipboard(generateMasterM3uUrl(credsPortal.serverUrl, credsPortal.username, credsPortal.password), 'masterM3u')}
                      className="p-1.5 bg-[#121217] hover:bg-[#222230] border border-[#262632] text-gray-300 hover:text-white rounded-lg transition cursor-pointer"
                      title="Copy Master M3U Link"
                    >
                      {copiedField === 'masterM3u' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              )}

              {/* Raw M3U Link if set */}
              {credsPortal.m3uUrl && (
                <div>
                  <label className="block text-[11px] font-sans font-semibold text-gray-400 mb-1">Custom M3U URL</label>
                  <div className="flex items-center gap-2 bg-[#181822] border border-[#262632] rounded-xl px-3 py-2">
                    <span className="text-gray-200 truncate flex-1 text-[11px]">{credsPortal.m3uUrl}</span>
                    <button
                      onClick={() => copyToClipboard(credsPortal.m3uUrl || '', 'rawM3u')}
                      className="p-1.5 bg-[#121217] hover:bg-[#222230] border border-[#262632] text-gray-300 hover:text-white rounded-lg transition cursor-pointer"
                      title="Copy Custom M3U URL"
                    >
                      {copiedField === 'rawM3u' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Quick Action Footer */}
            <div className="flex items-center justify-between pt-3 border-t border-[#262632] gap-3">
              <button
                onClick={() => {
                  const masterUrl = credsPortal.serverUrl && credsPortal.username && credsPortal.password
                    ? generateMasterM3uUrl(credsPortal.serverUrl, credsPortal.username, credsPortal.password)
                    : credsPortal.m3uUrl || '';
                  const allText = `Connection: ${credsPortal.name}\nServer: ${credsPortal.serverUrl}\nUser: ${credsPortal.username}\nPass: ${credsPortal.password}\nMaster M3U: ${masterUrl}`;
                  copyToClipboard(allText, 'all');
                }}
                className="px-4 py-2 bg-[#181822] hover:bg-[#20202c] border border-[#262632] text-gray-200 rounded-xl text-xs font-semibold flex items-center gap-2 transition cursor-pointer"
              >
                {copiedField === 'all' ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4 text-[#e50914]" />}
                <span>{copiedField === 'all' ? 'All Copied!' : 'Copy All Parameters'}</span>
              </button>

              <button
                onClick={() => setCredsPortal(null)}
                className="px-4 py-2 bg-[#e50914] hover:bg-[#ff1e27] text-white text-xs font-semibold rounded-xl transition cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Account Settings & Change Password Modal */}
      {isAccountModalOpen && user && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#121217] border border-[#262632] w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-5">
            {/* Modal Header */}
            <div className="flex items-center justify-between border-b border-[#262632] pb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-[#e50914]/20 text-[#ff1e27] rounded-xl border border-[#e50914]/30">
                  <KeyRound className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">Account & Password</h3>
                  <p className="text-xs text-gray-400">Manage your profile credentials and security</p>
                </div>
              </div>
              <button
                onClick={() => setIsAccountModalOpen(false)}
                className="text-gray-400 hover:text-white p-1 rounded-lg hover:bg-[#181822] transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Account Info Box */}
            <div className="bg-[#181822] rounded-xl p-3 border border-[#262632] space-y-1.5 text-xs font-mono">
              <div className="flex items-center justify-between text-gray-400">
                <span className="text-gray-500">Email:</span>
                <span className="text-gray-200">{user.email || 'Anonymous Session'}</span>
              </div>
              <div className="flex items-center justify-between text-gray-400">
                <span className="text-gray-500">User ID:</span>
                <span className="text-gray-300 truncate max-w-[200px]">{user.uid}</span>
              </div>
            </div>

            {/* Feedback Message */}
            {accountMessage && (
              <div
                className={`p-3 rounded-xl border text-xs flex items-center gap-2 ${
                  accountMessage.type === 'success'
                    ? 'bg-emerald-950/40 border-emerald-800 text-emerald-200'
                    : 'bg-red-950/40 border-red-800 text-red-200'
                }`}
              >
                {accountMessage.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
                )}
                <span>{accountMessage.text}</span>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleUpdateAccount} className="space-y-4">
              {/* Display Name */}
              <div>
                <label className="block text-xs font-semibold text-gray-300 mb-1">Display Name</label>
                <input
                  type="text"
                  value={accountDisplayName}
                  onChange={(e) => setAccountDisplayName(e.target.value)}
                  placeholder="e.g. Faraz Ahmad"
                  className="w-full bg-[#181822] border border-[#262632] rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-[#e50914] transition"
                />
              </div>

              {/* Password Section */}
              <div className="pt-2 border-t border-[#262632]/80 space-y-3">
                <div className="flex items-center justify-between">
                  <p className="text-xs font-semibold text-gray-300">Change Password</p>
                  <span className="text-[10px] text-gray-500">Leave blank to keep unchanged</span>
                </div>

                {user.email && (
                  <div>
                    <label className="block text-[11px] text-gray-400 mb-1">Current Password (Required to set new password)</label>
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      value={currentPasswordForChange}
                      onChange={(e) => setCurrentPasswordForChange(e.target.value)}
                      placeholder="••••••••"
                      className="w-full bg-[#181822] border border-[#262632] rounded-xl px-3.5 py-2 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-[#e50914] transition"
                    />
                  </div>
                )}

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-[11px] text-gray-400">New Password (Min 6 chars)</label>
                    <button
                      type="button"
                      onClick={() => setShowNewPassword(!showNewPassword)}
                      className="text-[11px] text-gray-400 hover:text-white flex items-center gap-1 transition cursor-pointer"
                    >
                      {showNewPassword ? <EyeOff className="w-3 h-3" /> : <Eye className="w-3 h-3" />}
                      <span>{showNewPassword ? 'Hide' : 'Show'}</span>
                    </button>
                  </div>
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter new password"
                    className="w-full bg-[#181822] border border-[#262632] rounded-xl px-3.5 py-2 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-[#e50914] transition"
                  />
                </div>

                <div>
                  <label className="block text-[11px] text-gray-400 mb-1">Confirm New Password</label>
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    value={confirmNewPassword}
                    onChange={(e) => setConfirmNewPassword(e.target.value)}
                    placeholder="Re-enter new password"
                    className="w-full bg-[#181822] border border-[#262632] rounded-xl px-3.5 py-2 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-[#e50914] transition"
                  />
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center justify-end gap-2 pt-3 border-t border-[#262632]">
                <button
                  type="button"
                  onClick={() => setIsAccountModalOpen(false)}
                  className="px-4 py-2 text-gray-400 hover:text-white text-xs font-semibold rounded-xl transition cursor-pointer"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={accountLoading}
                  className="px-5 py-2.5 bg-[#e50914] hover:bg-[#ff1e27] text-white text-xs font-semibold rounded-xl transition shadow-lg shadow-[#e50914]/25 flex items-center gap-2 cursor-pointer"
                >
                  {accountLoading && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                  <span>{accountLoading ? 'Saving...' : 'Save Account Settings'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* TV Device Pairing Modal */}
      {isPairModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#121217] border border-[#262632] w-full max-w-md rounded-2xl p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between border-b border-[#262632] pb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-blue-500/20 text-blue-400 rounded-xl border border-blue-500/30">
                  <QrCode className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">Pair Android TV Device</h3>
                  <p className="text-xs text-gray-400">Link your TV without typing credentials on a remote</p>
                </div>
              </div>
              <button
                onClick={() => {
                  setIsPairModalOpen(false);
                  setPairSuccess(null);
                  setPairError(null);
                }}
                className="text-gray-400 hover:text-white p-1 rounded-lg hover:bg-[#181822] transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {pairSuccess ? (
              <div className="bg-emerald-950/40 border border-emerald-800 rounded-xl p-4 text-center space-y-2">
                <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
                <p className="font-semibold text-sm text-emerald-200">{pairSuccess}</p>
                <button
                  onClick={() => setIsPairModalOpen(false)}
                  className="mt-3 px-5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold rounded-xl transition cursor-pointer"
                >
                  Done
                </button>
              </div>
            ) : (
              <form onSubmit={handleAuthorizeTv} className="space-y-4">
                <div className="bg-[#181822] rounded-xl p-3 border border-[#262632] space-y-1 text-xs">
                  <p className="text-gray-300 font-medium">How it works:</p>
                  <p className="text-gray-400">
                    1. On your TV, open the TVMime app and select <strong>"Quick Pair"</strong>.
                  </p>
                  <p className="text-gray-400">
                    2. Enter the 6-character code shown on your TV screen below.
                  </p>
                </div>

                {pairError && (
                  <div className="p-3 bg-red-950/50 border border-red-800 rounded-xl text-xs text-red-200 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 shrink-0 text-[#e50914]" />
                    <span>{pairError}</span>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-semibold text-gray-300 mb-1">TV Pairing Code</label>
                  <input
                    type="text"
                    value={pairCode}
                    onChange={(e) => setPairCode(e.target.value.toUpperCase())}
                    placeholder="e.g. MIME-4829"
                    maxLength={12}
                    className="w-full bg-[#181822] border border-[#262632] focus:border-blue-500 rounded-xl px-4 py-3 text-center text-lg font-mono tracking-widest text-white uppercase placeholder-gray-600 focus:outline-none transition"
                  />
                </div>

                {user && (
                  <div className="text-xs text-gray-400 flex items-center justify-between px-1">
                    <span>Linking to Account:</span>
                    <span className="font-mono text-gray-200 truncate max-w-[200px]">{user.email || user.displayName || 'Current User'}</span>
                  </div>
                )}

                <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#262632]">
                  <button
                    type="button"
                    onClick={() => setIsPairModalOpen(false)}
                    className="px-4 py-2 text-gray-400 hover:text-white text-xs font-semibold rounded-xl transition cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={pairLoading || !pairCode.trim()}
                    className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-semibold rounded-xl transition shadow-lg shadow-blue-600/25 flex items-center gap-2 cursor-pointer"
                  >
                    {pairLoading && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                    <span>{pairLoading ? 'Authorizing...' : 'Authorize TV Device'}</span>
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Stream Reports Modal */}
      {isReportsModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
          <div className="bg-[#121217] border border-[#262632] w-full max-w-2xl max-h-[85vh] rounded-2xl p-6 shadow-2xl flex flex-col space-y-4">
            <div className="flex items-center justify-between border-b border-[#262632] pb-4">
              <div className="flex items-center gap-2.5">
                <div className="p-2 bg-red-950/50 text-[#ff1e27] rounded-xl border border-red-800/60">
                  <AlertCircle className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">Stream Issue Reports ({streamReports.length})</h3>
                  <p className="text-xs text-gray-400">Critical playback and decoder failure reports logged from TV clients</p>
                </div>
              </div>
              <button
                onClick={() => setIsReportsModalOpen(false)}
                className="text-gray-400 hover:text-white p-1 rounded-lg hover:bg-[#181822] transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-3 pr-1">
              {streamReports.length === 0 ? (
                <div className="p-8 text-center text-gray-500 text-xs">
                  No stream errors reported yet. All channels are operating cleanly.
                </div>
              ) : (
                streamReports.map((r, index) => (
                  <div key={r.id || `report-${index}`} className="bg-[#181822] border border-[#262632] rounded-xl p-3.5 space-y-2 text-xs">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-white text-sm">{r.channelName}</span>
                        {r.channelNum && (
                          <span className="bg-[#121217] text-gray-400 font-mono text-[10px] px-1.5 py-0.5 rounded border border-[#262632]">
                            CH {r.channelNum}
                          </span>
                        )}
                      </div>
                      <span className="font-mono text-[10px] text-red-400 bg-red-950/40 border border-red-800/50 px-2 py-0.5 rounded-full font-bold">
                        {r.errorCode}
                      </span>
                    </div>

                    <p className="text-red-200 font-mono text-[11px] bg-[#121217] p-2 rounded-lg border border-[#262632]/80">
                      {r.errorMessage}
                    </p>

                    <div className="flex items-center justify-between text-[11px] text-gray-400 pt-1">
                      <span>{r.deviceSpecs || 'Android TV Client'}</span>
                      <span>{new Date(r.timestamp).toLocaleString()}</span>
                    </div>
                  </div>
                ))
              )}
            </div>

            <div className="flex justify-end pt-3 border-t border-[#262632]">
              <button
                onClick={() => setIsReportsModalOpen(false)}
                className="px-4 py-2 bg-[#181822] hover:bg-[#20202c] border border-[#262632] text-gray-200 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
