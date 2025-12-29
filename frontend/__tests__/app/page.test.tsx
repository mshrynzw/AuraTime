// jest.setup.jsでモックされているredirectを使用
const mockRedirect = (global as any).mockRedirect as jest.Mock;

describe("Home", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRedirect.mockClear();
  });

  it("正常系：ログインページにリダイレクトされる", async () => {
    // 動的インポートを使用してモックが適用された後にコンポーネントを読み込む
    const Home = (await import("@/app/page")).default;

    // Next.jsのredirectはエラーを投げるため、それを期待する
    expect(() => {
      Home();
    }).toThrow("NEXT_REDIRECT");

    // redirectが正しいURLで呼ばれたことを確認
    expect(mockRedirect).toHaveBeenCalledWith("/login");
    expect(mockRedirect).toHaveBeenCalledTimes(1);
  }, 10000); // タイムアウトを10秒に設定
});
