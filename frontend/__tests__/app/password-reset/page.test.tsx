import PasswordResetPage from "@/app/(public)/password-reset/page";
import { cleanup, render, screen } from "@testing-library/react";

// PasswordResetFormをモック
jest.mock("@/components/form/PasswordResetForm", () => {
  return function MockPasswordResetForm() {
    return <div>PasswordResetForm</div>;
  };
});

describe("PasswordResetPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("正常系：パスワードリセットページが表示される", () => {
    render(<PasswordResetPage />);

    expect(screen.getByText("AuraTime")).toBeInTheDocument();
    expect(screen.getByText("パスワードリセット")).toBeInTheDocument();
    expect(screen.getByText("PasswordResetForm")).toBeInTheDocument();
  });

  it("正常系：Cardコンポーネントが使用されている", () => {
    const { container } = render(<PasswordResetPage />);
    // Cardコンポーネントが使用されていることを確認（DOM構造から）
    const cardElement = container.querySelector('[class*="card"]');
    expect(cardElement).toBeInTheDocument();
  });
});
