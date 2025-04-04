// src/hooks/usePermissions.ts
import { useState, useEffect, useCallback } from "react";
import { fetchSession } from "../utils/session.utils";
import { UserRole, Permissions } from "../types/auth.types";
import { ROLE_PERMISSIONS } from "../utils/permissions.utils";
import { authUtils } from "../utils/auth.utils";

interface UsePermissionsResult {
  hasPermission: (permission: Permissions) => boolean;
  hasAnyPermission: (permissions: Permissions[]) => boolean;
  hasAllPermissions: (permissions: Permissions[]) => boolean;
  hasRole: (role: UserRole) => boolean;
  canRender: (required: Permissions[], children: React.ReactNode) => React.ReactNode | null;
  userPermissions: Set<Permissions>;
  isLoading: boolean;
  error: string | null;
}

const permissionCache = new Map<UserRole, Set<Permissions>>(); // Cache for computed role permissions

export function usePermissions(): UsePermissionsResult {
  const [userPermissions, setUserPermissions] = useState<Set<Permissions>>(new Set());
  const [role, setRole] = useState<UserRole | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("authToken");
    if (token) {
      setIsLoading(true);
      console.log("Loading permissions for this token", token.substring(0, 15) + "...");


      const userData = authUtils.getUser();
      console.log("Permissions data loaded: ", userData);



      if (userData){
        const userRole = userData.role as UserRole
        console.log("Setting userRole to: ", userRole);
        setRole(userRole);

        const rolePermissions = computeRolePermissions(userRole);
        const combinedPermissions = new Set(rolePermissions);


        if (userData.permissions && Array.isArray(userData.permissions)){
          userData.permissions.forEach((perm: Permissions) => combinedPermissions.add(perm));
        }
        setUserPermissions(combinedPermissions);
      }

      setIsLoading(false);
    }else{
      setIsLoading(false);
    }
    // old perm logic (before addition of extra details to token)
    //   fetchSession(token)
    //     .then((data) => {
    //       const userRole = data.role as UserRole;
    //       setRole(userRole);

    //       const rolePermissions = computeRolePermissions(userRole);
    //       const combinedPermissions = new Set(rolePermissions);

    //       data.permissions.forEach((perm: Permissions) => combinedPermissions.add(perm));
    //       setUserPermissions(combinedPermissions);
    //       setIsLoading(false);
    //     })
    //     .catch(() => {
    //       setError("Failed to fetch user session");
    //       setIsLoading(false);
    //     });
    // } else {
    //   setIsLoading(false);
    // }
  }, []);

  const computeRolePermissions = useCallback((role: UserRole): Set<Permissions> => {
    if (permissionCache.has(role)) {
      return permissionCache.get(role)!;
    }

    const roleConfig = ROLE_PERMISSIONS[role];
    if (!roleConfig) {
      throw new Error(`Invalid role: ${role}`);
    }

    const permissions = new Set(roleConfig.permissions);
    permissionCache.set(role, permissions);
    return permissions;
  }, []);

  const hasPermission = useCallback(
    (permission: Permissions): boolean => userPermissions.has(permission),
    [userPermissions]
  );

  const hasAnyPermission = useCallback(
    (permissions: Permissions[]): boolean => permissions.some((perm) => userPermissions.has(perm)),
    [userPermissions]
  );

  const hasAllPermissions = useCallback(
    (permissions: Permissions[]): boolean => permissions.every((perm) => userPermissions.has(perm)),
    [userPermissions]
  );

  const hasRole = useCallback((checkRole: UserRole): boolean => role === checkRole, [role]);

  const canRender = useCallback(
    (required: Permissions[], children: React.ReactNode): React.ReactNode | null => {
      return hasAnyPermission(required) ? children : null;
    },
    [hasAnyPermission]
  );

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    hasRole,
    canRender,
    userPermissions,
    isLoading,
    error,
  };
}

