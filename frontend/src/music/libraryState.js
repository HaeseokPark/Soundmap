import { createContext, useContext } from "react";

export const LibraryContext = createContext(null);

export function useLibrary() {
  const context = useContext(LibraryContext);
  if (!context) throw new Error("useLibrary는 LibraryProvider 안에서 사용해야 합니다.");
  return context;
}
