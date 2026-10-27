/** Ver 08-api-contract.md / openapi.yaml #UserSummary. */
export interface UserSummary {
  id: string
  username: string
  fullName: string
  role: 'ADMIN' | 'EMPLOYEE'
}

export interface VehicleType {
  id: string
  code: string
  name: string
}

export interface PaymentMethod {
  id: string
  code: string
  name: string
}

export type ParkingStatus = 'EN_PARQUEADERO' | 'FINALIZADO' | 'ANULADO'

/** Ver openapi.yaml #ParkingSessionSummary / #ParkingSessionDetail (unificados). */
export interface ParkingSession {
  id: string
  plate: string
  vehicleTypeCode: string | null
  vehicleTypeName: string | null
  entryDateTime: string
  exitDateTime: string | null
  elapsedMinutes: number
  entryEmployeeName: string | null
  status: ParkingStatus
  appliedTariffName: string | null
  calculatedAmount: number | null
  notes: string | null
  cancelReason: string | null
  /** Consecutivo del comprobante de ingreso (BR-027); null en sesiones anteriores. */
  entryNumber: string | null
  /** BR-029: mensualidad que cubre la sesión (null = cliente normal). */
  subscriptionId: string | null
  subscriptionCustomerName: string | null
  /** Solo al registrar el ingreso: aviso de mensualidad. */
  notice?: string | null
}

/** Una tarifa aplicable a la sesion, ya calculada (BR-008.1). */
export interface TariffOption {
  tariffId: string
  tariffName: string
  modality: string
  calculatedAmount: number
  /** true solo para la que el motor sugiere segun BR-008. */
  suggested: boolean
}

/** Los campos de primer nivel son la tarifa sugerida; `options` incluye todas las aplicables. */
export interface TariffQuote {
  /** null cuando la salida está cubierta por una mensualidad (BR-030). */
  tariffId: string | null
  tariffName: string | null
  modality: string | null
  elapsedMinutes: number
  calculatedAmount: number
  options: TariffOption[]
  /** BR-030: mensualidad de la sesión; si covered, la salida no se cobra. */
  subscription: SubscriptionCoverage | null
}

export interface SubscriptionCoverage {
  id: string
  customerName: string
  status: SubscriptionStatus
  paidUntil: string | null
  covered: boolean
}

export interface CheckoutResult {
  sessionId: string
  paymentId: string
  plate: string
  entryDateTime: string
  exitDateTime: string
  appliedTariffName: string
  modality: string
  calculatedAmount: number
  chargedAmount: number
  paymentMethodName: string
  operationNumber: string
  status: string
}

/** Ver openapi.yaml #CashSession — cubre tanto una caja abierta como una ya cerrada. */
export interface CashSession {
  id: string
  employeeId: string
  status: 'OPEN' | 'CLOSED'
  openedAt: string
  initialBalance: number
  closedAt: string | null
  cashCollected: number | null
  electronicCollected: number | null
  otherCollected: number | null
  totalCollected: number | null
  vehiclesServed: number | null
  expectedCash: number | null
  reportedCash: number | null
  difference: number | null
  /** BR-033: foto al cierre (0 mientras la caja está abierta). */
  otherIncomeCollected: number
  expensesTotal: number
}

export interface CashSessionSummary {
  initialBalance: number
  cashCollected: number
  electronicCollected: number
  otherCollected: number
  totalCollected: number
  vehiclesServed: number
  expectedCash: number
  /** BR-033: otros ingresos (ya incluidos en los totales por medio) y gastos; solo expensesCash resta del efectivo esperado. */
  otherIncomeCollected: number
  expensesTotal: number
  expensesCash: number
}

/** Fase 7 — Administración. */
export interface AdminUser {
  id: string
  username: string
  fullName: string
  role: 'ADMIN' | 'EMPLOYEE'
  active: boolean
}

export type TariffModality = 'FULL_AM' | 'FULL_PM' | 'FULL_DAY' | 'BY_TIME'
export type TariffRounding = 'UP' | 'DOWN' | 'NEAREST'
export type TariffTimeUnit = 'MINUTES' | 'HOURS'

export interface AdminTariff {
  id: string
  name: string
  description: string | null
  vehicleTypeId: string
  modality: TariffModality
  value: number
  /** BR-011: valor de la primera fracción (BY_TIME). null = todas las fracciones valen `value`. */
  firstFractionValue: number | null
  startTime: string | null
  endTime: string | null
  toleranceMinutes: number | null
  minTimeMinutes: number | null
  timeUnit: TariffTimeUnit | null
  fractionMinutes: number | null
  rounding: TariffRounding
  priority: number
  active: boolean
  validFrom: string
  validTo: string | null
}

export interface ParkingConfiguration {
  id: string
  parkingName: string
  nit: string | null
  address: string | null
  phone: string | null
  maxCapacity: number | null
  timezone: string
  currency: string
  receiptFormatJson: string | null
  subscriptionGraceDays: number
}

export type PaymentMethodCategory = 'CASH' | 'ELECTRONIC' | 'OTHER'

/** Fase 8 — Reportes y Dashboard. */
export interface Report {
  from: string
  to: string
  vehiclesEntered: number
  vehiclesFinalized: number
  enteredByVehicleType: Record<string, number>
  totalCollected: number
  collectedByPaymentMethod: Record<string, number>
  collectedByTariff: Record<string, number>
  averageDurationMinutes: number | null
  peakHour: number | null
  collectedByEmployee: Record<string, number>
  cancelledSessions: number
  cashDifferencesSum: number
  subscriptionCollected: number
  subscriptionPayments: number
  subscriberExits: number
  /** RF-083: otros ingresos (incluidos en totalCollected), gastos de caja y neto. */
  otherIncomeCollected: number
  expensesTotal: number
  netCollected: number
}

export interface RecentMovement {
  plate: string
  status: string
  description: string
}

export interface DashboardSummary {
  occupied: number
  maxCapacity: number | null
  entriesToday: number
  exitsToday: number
  collectedToday: number
  /** RF-084: otros ingresos de hoy (incluidos en collectedToday) y gastos de caja de hoy. */
  otherIncomeToday: number
  expensesToday: number
  openCashSessions: number
  recentMovements: RecentMovement[]
}

export interface AuditLogEntry {
  id: string
  userId: string
  userFullName: string
  action: string
  entityName: string
  entityId: string
  oldValue: string | null
  newValue: string | null
  occurredAt: string
}

/** RF-058: una fila del historial de sesiones (GET /parking-sessions). */
export interface HistoryItem {
  id: string
  plate: string
  vehicleTypeCode: string | null
  vehicleTypeName: string | null
  status: ParkingStatus
  entryDateTime: string
  exitDateTime: string | null
  elapsedMinutes: number
  entryEmployeeName: string | null
  exitEmployeeName: string | null
  appliedTariffName: string | null
  chargedAmount: number | null
  operationNumber: string | null
  /** Solo en sesiones cobradas: permite reimprimir el comprobante. */
  paymentId: string | null
  notes: string | null
  cancelReason: string | null
  entryNumber: string | null
  subscriptionCustomerName: string | null
}

export interface HistoryPage {
  items: HistoryItem[]
  /** true si hubo más resultados que el tope del servidor (acotar filtros). */
  truncated: boolean
}

/** RF-059: un cobro de una caja (GET /cash-sessions/{id}/payments). */
export interface CashOperation {
  paymentId: string
  operationNumber: string
  plate: string | null
  paidAt: string
  chargedAmount: number
  paymentMethodName: string | null
  tariffName: string | null
  /** PARKING = cobro de salida; SUBSCRIPTION = pago de mensualidad; INCOME / EXPENSE = otro ingreso o gasto (UC-014). */
  kind: 'PARKING' | 'SUBSCRIPTION' | CashTransactionType
  /** VOIDED = ingreso o gasto anulado (BR-035). */
  status: 'ACTIVE' | 'VOIDED'
  voidReason: string | null
}

/** UC-014: otros ingresos (cualquier rol) y gastos de caja (solo administrador). */
export type CashTransactionType = 'INCOME' | 'EXPENSE'

export interface CashConcept {
  id: string
  type: CashTransactionType
  name: string
  active: boolean
}

export interface CashTransaction {
  id: string
  type: CashTransactionType
  conceptId: string
  conceptName: string
  description: string | null
  amount: number
  paymentMethodName: string | null
  operationNumber: string
  userName: string | null
  cashSessionId: string
  cashierName: string | null
  createdAt: string
  status: 'ACTIVE' | 'VOIDED'
  voidReason: string | null
}

/** RF-082 (GET /cash-transactions/{id}/receipt). */
export interface CashTransactionReceipt {
  parkingName: string | null
  nit: string | null
  address: string | null
  phone: string | null
  transaction: CashTransaction
}

/** RF-081: caja abierta de la que el administrador puede registrar un gasto (GET /cash-sessions/open). */
export interface OpenCashSession {
  id: string
  employeeId: string
  employeeName: string | null
  openedAt: string
  expectedCash: number
}

/** RF-083 (GET /reports/cash-transactions). */
export interface CashTransactionReport {
  from: string
  to: string
  incomeTotal: number
  incomeCount: number
  expensesTotal: number
  expensesCount: number
  net: number
  voidedCount: number
  byConcept: { type: CashTransactionType; conceptName: string; count: number; total: number }[]
  transactions: CashTransaction[]
}

/** RF-057: caja cerrada, candidata a corrección administrativa (GET /cash-sessions). */
export interface ClosedCashSession {
  id: string
  employeeId: string
  employeeName: string | null
  openedAt: string
  closedAt: string
  initialBalance: number
  totalCollected: number | null
  otherIncomeCollected: number
  expensesTotal: number
  vehiclesServed: number | null
  expectedCash: number | null
  reportedCash: number | null
  difference: number | null
}

/** RF-050: datos del comprobante (GET /payments/{id}/receipt). */
export interface Receipt {
  parkingName: string | null
  nit: string | null
  address: string | null
  phone: string | null
  plate: string
  vehicleTypeName: string
  entryDateTime: string
  exitDateTime: string
  elapsedMinutes: number
  tariffName: string
  chargedAmount: number
  paymentMethodName: string
  operationNumber: string
  employeeName: string
}

/** RF-015: campos visibles del comprobante (Configuration.receiptFormatJson). */
export interface ReceiptFormat {
  showNit: boolean
  showAddress: boolean
  showPhone: boolean
  showEmployee: boolean
  showTariff: boolean
  showDuration: boolean
  footer: string
  /** Ofrecer imprimir comprobante al registrar el ingreso (RF-072). */
  printOnEntry: boolean
  /** Ofrecer imprimir comprobante al registrar la salida/cobro (RF-050). */
  printOnExit: boolean
}

/** RF-072: datos del comprobante de ingreso (GET /parking-sessions/{id}/entry-receipt). */
export interface EntryReceipt {
  parkingName: string | null
  nit: string | null
  address: string | null
  phone: string | null
  entryNumber: string | null
  plate: string
  vehicleTypeName: string | null
  entryDateTime: string
  employeeName: string | null
  notes: string | null
  status: ParkingStatus
}

/** RF-074 a RF-077: mensualidades. */
export type SubscriptionStatus = 'AL_DIA' | 'EN_GRACIA' | 'VENCIDA' | 'SIN_PAGO' | 'INACTIVA'

export interface Subscription {
  id: string
  customerName: string
  documentId: string | null
  phone: string | null
  monthlyFee: number
  cutOffDay: number
  paidUntil: string | null
  active: boolean
  notes: string | null
  status: SubscriptionStatus
  vehicleInsidePlate: string | null
  vehicles: { plate: string; vehicleTypeId: string }[]
}

export interface SubscriptionPayment {
  id: string
  subscriptionId: string
  operationNumber: string
  amount: number
  periodFrom: string
  periodTo: string
  paymentMethodId: string
  paidAt: string
}

export interface SubscriptionReceipt {
  parkingName: string | null
  nit: string | null
  address: string | null
  phone: string | null
  operationNumber: string
  customerName: string
  documentId: string | null
  plates: string[]
  periodFrom: string
  periodTo: string
  amount: number
  paymentMethodName: string | null
  employeeName: string | null
  paidAt: string
}

/** RF-078: reporte de mensualidades (GET /reports/subscriptions). */
export interface SubscriptionPortfolioRow {
  subscriptionId: string
  customerName: string
  documentId: string | null
  phone: string | null
  plates: string[]
  monthlyFee: number
  cutOffDay: number
  paidUntil: string | null
  status: SubscriptionStatus
  daysOverdue: number | null
  paidInPeriod: number
  paymentsInPeriod: number
  vehicleInsidePlate: string | null
}

export interface SubscriptionReportPaymentRow {
  paymentId: string
  paidAt: string
  operationNumber: string
  customerName: string
  documentId: string | null
  periodFrom: string
  periodTo: string
  amount: number
  paymentMethodName: string
  employeeName: string
}

export interface SubscriptionReport {
  from: string
  to: string
  today: string
  collected: number
  paymentsCount: number
  activeCount: number
  countByStatus: Record<SubscriptionStatus, number>
  overdueAmount: number
  portfolio: SubscriptionPortfolioRow[]
  payments: SubscriptionReportPaymentRow[]
}
