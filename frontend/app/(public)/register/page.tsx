import RegisterForm from "@/components/form/RegisterForm";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export const dynamic = "force-static";

export default function RegisterPage() {
  return (
    <Card className="w-full max-w-sm md:max-w-md shadow-lg border-none bg-gradient-to-br from-blue-50/50 via-white/60 to-blue-50/50 backdrop-blur-lg">
      <CardHeader className="text-center p-6 pb-0">
        <CardTitle className="text-3xl font-bold tracking-wider animate-dimlight box-reflect">
          AuraTime
        </CardTitle>
        <CardDescription className="text-base">
          新規ユーザー登録
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <RegisterForm />
      </CardContent>
    </Card>
  );
}
