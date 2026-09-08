import { useEffect, useRef, useState } from "react";
import { LibraryContext } from "./libraryState";
import { useAuth } from "../auth/AuthContext";
import { discoveryApi } from "../api/discovery";
export function LibraryProvider({ children }) {
  const { user } = useAuth();
  const userId = user?.id;
  const [ids, setIds] = useState(new Set());
  const [ready, setReady] = useState(false);
  const pending = useRef(new Set());
  useEffect(() => {
    let active = true;
    setIds(new Set());
    setReady(false);
    pending.current.clear();
    if (userId)
      discoveryApi
        .savedIds()
        .then((values) => {
          if (active) {
            setIds(new Set(values));
            setReady(true);
          }
        })
        .catch(() => {});
    return () => {
      active = false;
    };
  }, [userId]);
  const toggle = async (id) => {
    if (pending.current.has(id)) return;
    pending.current.add(id);
    try {
      let current = ids;
      if (!ready) {
        current = new Set(await discoveryApi.savedIds());
        setIds(current);
        setReady(true);
      }
      const wasSaved = current.has(id);
      await (wasSaved ? discoveryApi.unsave(id) : discoveryApi.save(id));
      setIds((prev) => {
        const next = new Set(prev);
        if (wasSaved) next.delete(id);
        else next.add(id);
        return next;
      });
    } finally {
      pending.current.delete(id);
    }
  };
  return (
    <LibraryContext.Provider value={{ ids, toggle, ready }}>
      {children}
    </LibraryContext.Provider>
  );
}
