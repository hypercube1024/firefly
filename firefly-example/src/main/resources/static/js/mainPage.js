$(document).ready(function () {
    var responseStatus = {
        OK: 1,
        ARGUMENT_ERROR: 2,
        SERVER_ERROR: 3
    };

    $("#productType").val($("#productTypeValue").val());

    $("#allProductBox").click(function () {
        $('input[name="productSubBox"]').prop("checked", this.checked);
    });

    $("#buyProducts").click(function () {
        $("#buyProducts").prop("disabled", true);

        var productBuyRequest = {
            products: []
        };
        $('input[name="productSubBox"]').filter('input:checked').each(function (index, value) {
            var product = {};
            product.productId = $(value).val();
            product.amount = $('#buyAmount_' + product.productId).val();
            productBuyRequest.products.push(product)
        });

        $.ajax({
            type: "POST",
            url: "/product/buy",
            dataType: "json",
            contentType: 'application/json; charset=utf-8',
            data: JSON.stringify(productBuyRequest)
        }).done(function (data) {
            if (data.status === responseStatus.OK) {
                $('#buySuccess').fadeIn("slow", function () {
                    $('#buySuccess').fadeOut("slow", function () {
                        window.location.href = "/" + window.location.search;
                    })
                })
            } else {
                $("#buyFailureContent").html(data.message);
                $("#buyFailure").fadeIn("slow", function () {
                    $("#buyFailure").fadeOut(1000, function () {
                        $("#buyProducts").prop("disabled", false);
                    });

                });
            }
        }).fail(function(jqXHR, textStatus, errorThrown) {
            $("#buyFailureContent").html(jqXHR.responseJSON.message);
            $("#buyFailure").fadeIn("slow", function () {
                $("#buyFailure").fadeOut(1000, function () {
                    $("#buyProducts").prop("disabled", false);
                });
            });
        });
    });
});