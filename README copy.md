# IU Canteen – giao diện JavaFX

Yêu cầu: JDK 17+ và Maven. Chạy:

    mvn javafx:run

Tài khoản demo (mật khẩu bất kỳ, email phải kết thúc bằng `iu.edu.vn`):

- `student@iu.edu.vn` → giao diện sinh viên / nhân viên
- `vendor@iu.edu.vn`  → giao diện quản lý quán
- `admin@iu.edu.vn`   → giao diện quản lý (nhãn Quản trị)

Cấu trúc source:

- `iu.canteen.App` – điểm khởi chạy và điều hướng giữa các màn hình
- `iu.canteen.ui` – đăng nhập/đăng ký, màn hình sinh viên/quản lý, component menu/kho và tiện ích JavaFX
- `iu.canteen.logic` – `Store` (mock data/model state/actions) và logic dự báo (`ForecastService`, `ForecastStrategy`, `ForecastMethod`)
- `src/main/resources/style.css` – giao diện

Các chỗ cần nối backend/DB đều được đánh dấu `// TODO`.
