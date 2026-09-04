const cpfInput = document.querySelector("[data-cpf]");
const submitButton = document.querySelector("[data-submit]");
const form = document.querySelector(".validation-form");
const loadingOverlay = document.querySelector("[data-loading-overlay]");
const submitButtonContent = submitButton?.innerHTML;

if (cpfInput) {
	cpfInput.addEventListener("input", () => {
		const digits = cpfInput.value.replace(/\D/g, "").slice(0, 11);
		cpfInput.value = digits
			.replace(/(\d{3})(\d)/, "$1.$2")
			.replace(/(\d{3})(\d)/, "$1.$2")
			.replace(/(\d{3})(\d{1,2})$/, "$1-$2");
	});
}

document.querySelectorAll(".upload-zone").forEach((uploadZone) => {
	const fileInput = uploadZone.querySelector("input[type='file']");
	const fileName = uploadZone.querySelector("[data-file-name]");
	const defaultText = fileName?.dataset.fileDefault || fileName?.textContent || "";

	if (!fileInput || !fileName) {
		return;
	}

	fileInput.addEventListener("change", () => {
		const file = fileInput.files?.[0];
		fileName.textContent = file ? file.name : defaultText;
		uploadZone.classList.toggle("is-active", Boolean(file));
	});
});

if (form && submitButton) {
	form.addEventListener("submit", () => {
		if (!form.checkValidity()) {
			return;
		}

		submitButton.classList.add("is-loading");
		submitButton.disabled = true;
		submitButton.textContent = "Analisando documento e biometria...";
		document.body.classList.add("is-validating");

		if (loadingOverlay) {
			loadingOverlay.hidden = false;
			loadingOverlay.setAttribute("aria-hidden", "false");
		}
	});
}

window.addEventListener("pageshow", () => {
	document.body.classList.remove("is-validating");

	if (loadingOverlay) {
		loadingOverlay.hidden = true;
		loadingOverlay.setAttribute("aria-hidden", "true");
	}

	if (submitButton) {
		submitButton.disabled = false;
		submitButton.classList.remove("is-loading");
		if (submitButtonContent) {
			submitButton.innerHTML = submitButtonContent;
		}
	}
});
