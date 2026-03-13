const PHONE_PATTERN = /^\+?[0-9][0-9\s-]{9,14}$/;
const OTP_PATTERN = /^[0-9]{6}$/;
const NAME_PATTERN = /^[A-Za-z][A-Za-z\s'-]{0,49}$/;

function required(label) {
  return { required: `${label} is required` };
}

function withMinLength(rule, value, message) {
  return { ...rule, minLength: { value, message } };
}

function withMaxLength(rule, value, message) {
  return { ...rule, maxLength: { value, message } };
}

function withPattern(rule, pattern, message) {
  return { ...rule, pattern: { value: pattern, message } };
}

export function errorIdFor(fieldName) {
  return `${fieldName}-error`;
}

export function getInputA11y(fieldName, errors) {
  const hasError = Boolean(errors?.[fieldName]);
  return {
    "aria-invalid": hasError ? "true" : "false",
    "aria-describedby": hasError ? errorIdFor(fieldName) : undefined,
  };
}

export const authRules = {
  phone: withPattern(
    withMinLength(required("Phone number"), 10, "Enter a valid phone number"),
    PHONE_PATTERN,
    "Use a valid phone number format"
  ),
  otp: withPattern(
    withMaxLength(withMinLength(required("OTP"), 6, "OTP should be 6 digits"), 6, "OTP should be 6 digits"),
    OTP_PATTERN,
    "OTP should contain only numbers"
  ),
};

export const onboardingRules = {
  step1: {
    profile_created_for: required("Profile created for"),
    first_name: withPattern(
      withMinLength(required("First name"), 2, "First name should be at least 2 characters"),
      NAME_PATTERN,
      "Use letters only"
    ),
    last_name: withPattern(
      withMinLength(required("Last name"), 2, "Last name should be at least 2 characters"),
      NAME_PATTERN,
      "Use letters only"
    ),
    date_of_birth: required("Date of birth"),
    gender: required("Gender"),
    marital_status: required("Marital status"),
  },
  step2: {
    country: withMinLength(required("Country"), 2, "Country should be at least 2 characters"),
    state: withMinLength(required("State"), 2, "State should be at least 2 characters"),
    city: withMinLength(required("City"), 2, "City should be at least 2 characters"),
    religion: required("Religion"),
    mother_tongue: withMinLength(required("Mother tongue"), 2, "Mother tongue should be at least 2 characters"),
  },
  step3: {
    education_level: required("Education level"),
    profession: withMinLength(required("Profession"), 2, "Profession should be at least 2 characters"),
    income_band: required("Income"),
  },
  step4: {
    food_habit: required("Dietary preference"),
    bio: withMaxLength(
      withMinLength(required("Bio"), 20, "Bio must be at least 20 characters"),
      500,
      "Bio cannot exceed 500 characters"
    ),
  },
  step5: {
    min_age: {
      required: "Min age is required",
      min: { value: 18, message: "Min age should be 18 or more" },
      max: { value: 70, message: "Min age should be 70 or less" },
    },
    max_age: {
      required: "Max age is required",
      min: { value: 18, message: "Max age should be 18 or more" },
      max: { value: 70, message: "Max age should be 70 or less" },
    },
  },
};

export const settingsRules = {
  password: {
    currentPassword: withMinLength(required("Current password"), 6, "Current password should be at least 6 characters"),
    newPassword: withMinLength(required("New password"), 8, "New password should be at least 8 characters"),
  },
};
