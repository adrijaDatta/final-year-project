// Registration Page Functionality
document.addEventListener("DOMContentLoaded", function () {
  // Registration form handling
  const registrationForm = document.getElementById("registration-form");
  if (registrationForm) {
    registrationForm.addEventListener("submit", function (e) {
      e.preventDefault();

      // Basic validation
      const password = document.getElementById("password").value;
      const confirmPassword = document.getElementById("confirm-password").value;

      if (password !== confirmPassword) {
        alert("Passwords do not match!");
        return;
      }

      // In a real application, you would send the data to a server
      alert("Registration successful! You can now login with OTP.");
      window.location.href = "login.html";
    });

    // Fingerprint scanning simulation
    const scanButtons = document.querySelectorAll(
      "#scan-fingerprints, #scan-nominee-fingerprints"
    );
    scanButtons.forEach((button) => {
      button.addEventListener("click", function () {
        const isNominee = this.id === "scan-nominee-fingerprints";
        const fingerprintSlots = document.querySelectorAll(
          isNominee
            ? ".fingerprint-slot.nominee"
            : ".fingerprint-slot:not(.nominee)"
        );

        // Simulate scanning process
        let scannedCount = 0;
        const scanInterval = setInterval(() => {
          if (scannedCount < fingerprintSlots.length) {
            fingerprintSlots[scannedCount].classList.add("scanned");
            fingerprintSlots[scannedCount].textContent =
              "✓ " +
              fingerprintSlots[scannedCount]
                .getAttribute("data-finger")
                .replace("-", " ");
            scannedCount++;
          } else {
            clearInterval(scanInterval);
            alert(
              isNominee
                ? "Nominee fingerprint scanning completed!"
                : "User fingerprint scanning completed!"
            );
          }
        }, 500);
      });
    });
  }

  // Login Page Functionality
  const loginForm = document.getElementById("login-form");
  if (loginForm) {
    const requestOtpBtn = document.getElementById("request-otp");
    const otpSection = document.getElementById("otp-section");
    const loginBtn = document.getElementById("login-btn");

    requestOtpBtn.addEventListener("click", function () {
      const email = document.getElementById("login-email").value;

      if (!email) {
        alert("Please enter your email address");
        return;
      }

      // Simulate OTP request
      requestOtpBtn.textContent = "Sending OTP...";
      requestOtpBtn.disabled = true;

      setTimeout(() => {
        requestOtpBtn.textContent = "OTP Sent!";
        otpSection.style.display = "block";
        loginBtn.style.display = "block";

        // In a real application, you would send OTP to the user's email/phone
        alert("OTP has been sent to your registered email and phone number");
      }, 2000);
    });

    loginForm.addEventListener("submit", function (e) {
      e.preventDefault();

      const otp = document.getElementById("otp").value;

      if (!otp) {
        alert("Please enter the OTP");
        return;
      }

      // Simulate OTP verification
      alert("Login successful!");
      // In a real application, you would redirect to the user dashboard
    });
  }
});
