import LoginForm from "@/components/form/LoginForm";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import Image from "next/image";

export const dynamic = "force-static";

export default function LoginPage() {
  return (
    <Card className="w-full max-w-sm md:max-w-md shadow-lg border-none bg-gradient-to-br from-blue-50/50 via-white/60 to-blue-50/50 backdrop-blur-lg">
      <CardHeader className="text-center">
        <div className="flex justify-center">
          <div className="flex items-center justify-center">
            <Image
              src="/logo.png"
              alt="AuraTime"
              width={256}
              height={256}
              loading="eager"
              priority
            />
          </div>
        </div>
        <div>
          <CardTitle className="text-3xl font-bold tracking-wider animate-dimlight box-reflect">
            AuraTime
          </CardTitle>
          <CardDescription className="text-base">Version 1.0.0</CardDescription>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <LoginForm />
      </CardContent>
    </Card>
  );
}
