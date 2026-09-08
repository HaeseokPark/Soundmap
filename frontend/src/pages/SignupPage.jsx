import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import Logo from "../components/Logo";

const EMPTY_FORM = { email: "", password: "", passwordConfirm: "", nickname: "" };

export default function SignupPage() {
  const { signup, isAuthenticated, loading } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  if (loading) return <div className="screen-center">불러오는 중...</div>;
  if (isAuthenticated) return <Navigate to="/" replace />;

  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});

    if (form.password !== form.passwordConfirm) {
      setFieldErrors({ passwordConfirm: "비밀번호가 서로 다릅니다." });
      return;
    }

    setSubmitting(true);
    try {
      await signup({
        email: form.email,
        password: form.password,
        nickname: form.nickname,
      });
      navigate("/", { replace: true });
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors ?? {});
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="screen-center">
      <form className="card" onSubmit={handleSubmit}>
        <Logo size={46} />
        <h1 className="logo">회원가입</h1>
        <p className="subtitle">음악 취향으로 주변 사람을 만나보세요.</p>

        <label className="field">
          <span>이메일</span>
          <input
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            autoComplete="email"
            required
          />
          {fieldErrors.email && <em className="field-error">{fieldErrors.email}</em>}
        </label>

        <label className="field">
          <span>닉네임</span>
          <input
            name="nickname"
            value={form.nickname}
            onChange={handleChange}
            placeholder="2~10자, 한글/영문/숫자"
            required
          />
          {fieldErrors.nickname && <em className="field-error">{fieldErrors.nickname}</em>}
        </label>

        <label className="field">
          <span>비밀번호</span>
          <input
            name="password"
            type="password"
            value={form.password}
            onChange={handleChange}
            placeholder="8자 이상"
            autoComplete="new-password"
            required
          />
          {fieldErrors.password && <em className="field-error">{fieldErrors.password}</em>}
        </label>

        <label className="field">
          <span>비밀번호 확인</span>
          <input
            name="passwordConfirm"
            type="password"
            value={form.passwordConfirm}
            onChange={handleChange}
            autoComplete="new-password"
            required
          />
          {fieldErrors.passwordConfirm && (
            <em className="field-error">{fieldErrors.passwordConfirm}</em>
          )}
        </label>

        {error && <p className="error">{error}</p>}

        <button className="primary" type="submit" disabled={submitting}>
          {submitting ? "가입 중..." : "가입하기"}
        </button>

        <p className="hint">
          이미 계정이 있으신가요? <Link to="/login">로그인</Link>
        </p>
      </form>
    </div>
  );
}
