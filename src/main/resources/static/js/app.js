const cpfInput = document.querySelector("[data-cpf]");
const fileInput = document.querySelector("#documento");
const fileName = document.querySelector("[data-file-name]");
const uploadZone = document.querySelector(".upload-zone");
const submitButton = document.querySelector("[data-submit]");
const form = document.querySelector(".validation-form");

if (cpfInput) {
	cpfInput.addEventListener("input", () => {
		const digits = cpfInput.value.replace(/\D/g, "").slice(0, 11);
		cpfInput.value = digits
			.replace(/(\d{3})(\d)/, "$1.$2")
			.replace(/(\d{3})(\d)/, "$1.$2")
			.replace(/(\d{3})(\d{1,2})$/, "$1-$2");
	});
}

if (fileInput && fileName && uploadZone) {
	fileInput.addEventListener("change", () => {
		const file = fileInput.files?.[0];
		fileName.textContent = file ? file.name : "Anexe RG ou CNH";
		uploadZone.classList.toggle("is-active", Boolean(file));
	});
}

if (form && submitButton) {
	form.addEventListener("submit", () => {
		submitButton.classList.add("is-loading");
		submitButton.textContent = "Analisando documento...";
	});
}
