var menuBtn = document.getElementById("menuBtn");
var menuList = document.getElementById("menuList");
var menuValue = document.getElementById("menuValue");

menuBtn.addEventListener("click", function () {
  menuList.classList.toggle("hidden");
});

menuList.addEventListener("click", function (e) {
  if (e.target.tagName !== "LI") return;
  menuList.querySelectorAll("li").forEach(function (li) {
    li.classList.remove("selected");
  });
  e.target.classList.add("selected");
  menuValue.textContent = e.target.textContent;
  menuList.classList.add("hidden");
});

document.addEventListener("click", function (e) {
  if (!e.target.closest(".menu-demo")) {
    menuList.classList.add("hidden");
  }
});

var glassToggle = document.getElementById("glassToggle");
glassToggle.addEventListener("click", function () {
  var on = glassToggle.getAttribute("aria-pressed") === "true";
  glassToggle.setAttribute("aria-pressed", on ? "false" : "true");
});
