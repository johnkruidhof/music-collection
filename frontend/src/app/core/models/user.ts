export interface User {
  id: string;
  username: string;
  email: string | null;
  displayName: string | null;
  roles: string[];
}
