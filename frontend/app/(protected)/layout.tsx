import AuthGuard from "@/components/layout/AuthGuard";
import Navbar from "@/components/layout/Navbar";
import NavigationLoader from "@/components/layout/NavigationLoader";

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard>
      <NavigationLoader />
      <Navbar />
      {children}
    </AuthGuard>
  );
}
