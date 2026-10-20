// Thin data-access layer. For now this serves the mock fixtures in mockData.ts so all four
// screens are fully demoable without any backend running. Once the real services are deployed,
// each function body should be swapped to `fetch(`${API_BASE}/api/...`)` against the exact
// shapes in docs/api-specification.md — the screens themselves should not need to change, since
// they only depend on the TypeScript types in types.ts, not on how the data was fetched.

import {
  mockAuditEvents,
  mockCurrentSchedule,
  mockLaunchReadiness,
  mockMaterials,
  mockProposals,
  mockResources,
  mockRockets,
  mockWorkOrders,
} from './mockData'
import type {
  AuditEvent,
  LaunchPreparation,
  Material,
  Resource,
  Rocket,
  ScheduleProposal,
  ScheduleVersion,
  WorkOrder,
} from './types'

export const API_BASE = import.meta.env.VITE_API_URL ?? ''
export const USING_MOCK_DATA = !API_BASE

const delay = <T,>(value: T, ms = 220): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), ms))

export async function fetchRockets(): Promise<Rocket[]> {
  return delay(mockRockets)
}

export async function fetchWorkOrders(filters?: { rocketId?: number }): Promise<WorkOrder[]> {
  const all = await delay(mockWorkOrders)
  if (filters?.rocketId == null) return all
  return all.filter((wo) => wo.rocketId === filters.rocketId)
}

export async function fetchResources(): Promise<Resource[]> {
  return delay(mockResources)
}

export async function fetchMaterials(): Promise<Material[]> {
  return delay(mockMaterials)
}

export async function fetchCurrentSchedule(): Promise<ScheduleVersion> {
  return delay(mockCurrentSchedule)
}

export async function fetchProposals(status?: string): Promise<ScheduleProposal[]> {
  const all = await delay(mockProposals)
  if (!status) return all
  return all.filter((p) => p.status === status)
}

export async function fetchProposal(id: number): Promise<ScheduleProposal | undefined> {
  const all = await delay(mockProposals)
  return all.find((p) => p.id === id)
}

export async function fetchLaunchReadiness(): Promise<LaunchPreparation[]> {
  return delay(mockLaunchReadiness)
}

export async function fetchAuditEvents(): Promise<AuditEvent[]> {
  return delay(mockAuditEvents)
}

/**
 * Approve or reject a proposal. In mock mode this mutates the in-memory fixture so the UI can
 * demonstrate the full approve/reject interaction without a backend; it does not persist across
 * a page reload. Against the real API this calls `POST /api/proposals/{id}/approve` or
 * `/reject` with `{ approvedBy }`.
 */
export async function decideProposal(
  id: number,
  decision: 'APPROVE' | 'REJECT',
  approvedBy: string,
): Promise<ScheduleProposal> {
  const proposal = mockProposals.find((p) => p.id === id)
  if (!proposal) throw new Error(`Proposal ${id} not found`)
  proposal.status = decision === 'APPROVE' ? 'APPROVED' : 'REJECTED'
  proposal.decidedAt = new Date().toISOString()
  proposal.decidedBy = approvedBy
  if (decision === 'APPROVE') {
    mockCurrentSchedule.assignments = proposal.agentFindings.schedulingOptimization.candidates[0].assignments
    mockCurrentSchedule.status = 'PUBLISHED'
  }
  return delay(proposal, 350)
}
