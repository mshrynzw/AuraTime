import PasswordResetForm from "@/components/form/PasswordResetForm";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export const dynamic = "force-static";

export default function PasswordResetPage() {
  return (
    <Card className="w-full max-w-sm md:max-w-md shadow-lg border-none bg-gradient-to-br from-blue-50/50 via-white/60 to-blue-50/50 backdrop-blur-lg">
      <CardHeader className="text-center p-6 pb-0">
        <CardTitle className="text-3xl font-bold tracking-wider animate-dimlight box-reflect">
          AuraTime
        </CardTitle>
        <CardDescription className="text-base">
          パスワードリセット
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <PasswordResetForm />
      </CardContent>
    </Card>
  );
}
