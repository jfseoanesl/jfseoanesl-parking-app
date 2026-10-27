import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { ToastProvider } from './components/Toast'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppShell } from './layout/AppShell'
import LoginPage from './pages/LoginPage'
import HomePage from './pages/HomePage'
import NewEntryPage from './pages/NewEntryPage'
import ActiveSessionsPage from './pages/ActiveSessionsPage'
import CheckoutPage from './pages/CheckoutPage'
import CashRegisterPage from './pages/CashRegisterPage'
import UsersPage from './pages/UsersPage'
import TariffsPage from './pages/TariffsPage'
import ConfigurationPage from './pages/ConfigurationPage'
import AuditLogPage from './pages/AuditLogPage'
import ReportsPage from './pages/ReportsPage'
import BackupPage from './pages/BackupPage'
import SubscriptionsPage from './pages/SubscriptionsPage'
import SubscriptionReceiptPage from './pages/SubscriptionReceiptPage'
import CashTransactionReceiptPage from './pages/CashTransactionReceiptPage'
import CashTransactionsPage from './pages/CashTransactionsPage'
import EntryReceiptPage from './pages/EntryReceiptPage'
import ReceiptPage from './pages/ReceiptPage'
import HistoryPage from './pages/HistoryPage'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <AppShell />
                </ProtectedRoute>
              }
            >
              <Route index element={<HomePage />} />
              <Route path="ingreso" element={<NewEntryPage />} />
              <Route path="parqueados" element={<ActiveSessionsPage />} />
              <Route path="cobros/:sessionId" element={<CheckoutPage />} />
              <Route path="caja" element={<CashRegisterPage />} />
              <Route path="ingresos-gastos" element={<CashTransactionsPage />} />
              <Route path="usuarios" element={<UsersPage />} />
              <Route path="tarifas" element={<TariffsPage />} />
              <Route path="configuracion" element={<ConfigurationPage />} />
              <Route path="auditoria" element={<AuditLogPage />} />
              <Route path="respaldo" element={<BackupPage />} />
              <Route path="mensualidades" element={<SubscriptionsPage />} />
              <Route path="comprobantes-mensualidad/:paymentId" element={<SubscriptionReceiptPage />} />
              <Route path="comprobantes-movimiento/:transactionId" element={<CashTransactionReceiptPage />} />
              <Route path="reportes" element={<ReportsPage />} />
              <Route path="historial" element={<HistoryPage />} />
              <Route path="comprobantes/:paymentId" element={<ReceiptPage />} />
              <Route path="comprobantes-ingreso/:sessionId" element={<EntryReceiptPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
