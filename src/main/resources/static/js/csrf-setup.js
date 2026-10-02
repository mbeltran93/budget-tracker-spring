/**
 * Agrega el header CSRF (leido de las <meta> que renderiza Thymeleaf) a toda
 * peticion $.ajax que no sea GET/HEAD/OPTIONS/TRACE. Spring Security exige ese
 * token en cada POST/PUT/DELETE aunque la llamada sea asincronica.
 */
$(function () {
    var token = $('meta[name="_csrf"]').attr('content');
    var header = $('meta[name="_csrf_header"]').attr('content');

    if (!token || !header) {
        return;
    }

    $.ajaxSetup({
        beforeSend: function (xhr, settings) {
            if (!/^(GET|HEAD|OPTIONS|TRACE)$/i.test(settings.type)) {
                xhr.setRequestHeader(header, token);
            }
        }
    });
});
