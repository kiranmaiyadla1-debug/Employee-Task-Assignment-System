// Minimal JavaScript for the Thymeleaf pages (no framework).
document.addEventListener('DOMContentLoaded', function () {

    // 1) Ask for confirmation before submitting forms that carry data-confirm="...".
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm(form.getAttribute('data-confirm'))) {
                event.preventDefault();
            }
        });
    });

    // 2) Changing the status drop-down in the task table saves the new status immediately.
    document.querySelectorAll('.status-select').forEach(function (select) {
        select.addEventListener('change', function () {
            select.form.submit();
        });
    });

    // 3) Success messages disappear after a few seconds (error messages stay).
    document.querySelectorAll('.alert.auto-hide').forEach(function (alertBox) {
        setTimeout(function () {
            alertBox.classList.remove('show');
            setTimeout(function () { alertBox.remove(); }, 400);
        }, 6000);
    });

    // 4) Bring an error message into view (useful on long forms).
    var error = document.getElementById('errorAlert');
    if (error) {
        error.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
});
