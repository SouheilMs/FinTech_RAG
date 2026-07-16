export interface AppUser {
    id: string
    username: string
    email: string
    firstName: string
    lastName: string
    enabled: boolean
    roles: string[]
    createdTimestamp: number | null
}

export interface CreateUserPayload {
    username:  string
    email: string
    firstName: string
    lastName:  string
    password:  string
}

export interface UpdateUserPayload {
    username:  string
    email: string
    firstName: string
    lastName: string
    enabled: boolean
}

export interface ResetPasswordPayload {
    password: string
    temporary: boolean
}

export const AVAILABLE_ROLES = ['ADMIN', 'USER'] as const
export type  RoleName = typeof AVAILABLE_ROLES[number]