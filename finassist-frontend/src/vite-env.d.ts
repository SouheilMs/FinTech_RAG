interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_KEYCLOAK_URL?: string
  readonly VITE_KEYCLOAK_REALM?: string
  readonly VITE_KEYCLOAK_CLIENT?: string
  readonly VITE_DEV_API_TARGET?: string
  readonly VITE_DEV_SERVER_PORT?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
