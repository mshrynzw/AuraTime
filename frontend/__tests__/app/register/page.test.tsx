import RegisterPage from "@/app/(public)/register/page";
import { cleanup, render, screen } from "@testing-library/react";

// RegisterFormをモック
jest.mock("@/components/form/RegisterForm", () => {
  return function MockRegisterForm() {
    return <div>RegisterForm</div>;
  };
});

describe("RegisterPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("正常系：登録ページが表示される", () => {
    render(<RegisterPage />);

    expect(screen.getByText("AuraTime")).toBeInTheDocument();
    expect(screen.getByText("新規ユーザー登録")).toBeInTheDocument();
    expect(screen.getByText("RegisterForm")).toBeInTheDocument();
  });

  it("正常系：Cardコンポーネントが使用されている", () => {
    const { container } = render(<RegisterPage />);
    // Cardコンポーネントが使用されていることを確認（DOM構造から）
    const cardElement = container.querySelector('[class*="card"]');
    expect(cardElement).toBeInTheDocument();
  });
});
