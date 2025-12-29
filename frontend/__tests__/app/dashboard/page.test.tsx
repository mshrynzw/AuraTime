import DashboardPage from "@/app/(protected)/dashboard/page";
import { cleanup, render, screen } from "@testing-library/react";

// DashboardContentをモック
jest.mock("@/app/(protected)/dashboard/DashboardContent", () => {
  return function MockDashboardContent() {
    return <div>DashboardContent</div>;
  };
});

describe("DashboardPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  afterEach(() => {
    cleanup();
  });

  it("正常系：ダッシュボードページが表示される", () => {
    render(<DashboardPage />);

    expect(screen.getByText("ダッシュボード")).toBeInTheDocument();
    expect(screen.getByText("DashboardContent")).toBeInTheDocument();
  });

  it("正常系：mainタグが使用されている", () => {
    const { container } = render(<DashboardPage />);
    const mainElement = container.querySelector("main");
    expect(mainElement).toBeInTheDocument();
  });
});
