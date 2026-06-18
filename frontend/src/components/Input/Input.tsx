import type { InputHTMLAttributes } from 'react';
import styles from './Input.module.css';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  icon?: string;
}

export function Input({ label, error, icon, id, className, ...rest }: InputProps) {
  return (
    <div className={styles.formGroup}>
      {label && (
        <label htmlFor={id} className={styles.label}>
          {label}
          {rest.required && <span className={styles.required}>*</span>}
        </label>
      )}
      <div className={styles.inputWrapper}>
        {icon && <i className={[icon, styles.icon].join(' ')} aria-hidden />}
        <input
          id={id}
          className={[styles.input, icon ? styles.withIcon : '', error ? styles.hasError : '', className ?? ''].join(' ')}
          {...rest}
        />
      </div>
      {error && <span className={styles.error}>{error}</span>}
    </div>
  );
}
