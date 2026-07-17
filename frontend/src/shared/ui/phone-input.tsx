import PhoneInputBase, { isValidPhoneNumber } from "react-phone-number-input";
import "react-phone-number-input/style.css";

export { isValidPhoneNumber };

type PhoneInputProps = {
  value: string;
  onChange: (value: string) => void;
  id?: string;
  placeholder?: string;
  required?: boolean;
  className?: string;
};

/**
 * International phone number input — country auto-detect, flag, live formatting, "+" locked
 * in place, E.164 output (e.g. "+77071234567"). Wraps react-phone-number-input
 * (libphonenumber-js under the hood) styled to match the site's underlined form fields.
 */
export function PhoneInput({
  value,
  onChange,
  id,
  placeholder,
  required,
  className,
}: PhoneInputProps) {
  return (
    <PhoneInputBase
      id={id}
      defaultCountry="KZ"
      international
      countryCallingCodeEditable={false}
      placeholder={placeholder}
      value={value || undefined}
      onChange={(next) => onChange(next ?? "")}
      className={
        "flex items-center gap-2 border-b border-[--color-border] bg-transparent py-3 " +
        "transition focus-within:border-black" +
        (className ? ` ${className}` : "")
      }
      numberInputProps={{
        required,
        className:
          "w-full min-w-0 flex-1 bg-transparent text-sm text-black outline-none placeholder:text-[--color-muted]",
      }}
    />
  );
}
