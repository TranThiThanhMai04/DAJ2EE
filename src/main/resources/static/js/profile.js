/**
 * File xử lý các nghiệp vụ cho trang Quản lý Hồ sơ cá nhân (Profile)
 */

// Hàm dùng chung để lấy CSRF Token từ thẻ meta trong HTML
function getCsrfConfig() {
    const tokenMeta = document.querySelector("meta[name='_csrf']");
    const headerMeta = document.querySelector("meta[name='_csrf_header']");
    
    const headers = {
        'Content-Type': 'application/json'
    };
    
    if (tokenMeta && headerMeta) {
        headers[headerMeta.getAttribute("content")] = tokenMeta.getAttribute("content");
    }
    return headers;
}

/**
 * Chức năng 1: Cập nhật thông tin cá nhân (Chỉ Họ tên)
 * Nghiệp vụ: Khóa SĐT và Email, cư dân chỉ được đổi Họ tên.
 */
async function updateProfile() {
    const fullName = document.getElementById('fullName').value;

    if (!fullName) {
        Swal.fire('Thiếu thông tin!', 'Vui lòng nhập Họ tên.', 'warning');
        return;
    }

    try {
        const response = await fetch('/api/profile/update', {
            method: 'POST',
            headers: getCsrfConfig(),
            body: JSON.stringify({ fullName })
        });
        
        const result = await response.json();
        if (response.ok) {
            Swal.fire('Thành công!', 'Hồ sơ đã được cập nhật.', 'success').then(() => location.reload());
        } else {
            // Hiển thị lỗi từ backend (Validation hoặc Business Logic)
            let errorMsg = result.message || 'Có lỗi xảy ra khi cập nhật.';
            if (typeof result === 'object' && !result.message) {
                errorMsg = Object.values(result).join('<br>');
            }
            Swal.fire({
                icon: 'error',
                title: 'Lỗi!',
                html: errorMsg
            });
        }
    } catch (error) {
        console.error("Error:", error);
        Swal.fire('Lỗi kết nối!', 'Không thể kết nối đến máy chủ.', 'error');
    }
}

/**
 * Chức năng 2: Đổi mật khẩu
 * Ràng buộc: 
 * - Không bỏ trống 3 ô.
 * - Độ dài tối thiểu 8 ký tự.
 * - Phải có chữ hoa, thường, số, ký tự đặc biệt (Regex giống backend).
 * - Mật khẩu mới và Xác nhận phải khớp nhau (JS check).
 */
async function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;

    // 1. Kiểm tra bỏ trống
    if (!currentPassword || !newPassword || !confirmPassword) {
        Swal.fire('Cảnh báo!', 'Vui lòng điền đầy đủ các trường mật khẩu.', 'warning');
        return;
    }

    // 2. Kiểm tra độ dài tối thiểu (8 ký tự theo chuẩn đăng ký)
    if (newPassword.length < 8) {
        Swal.fire('Độ dài yếu!', 'Mật khẩu mới phải có ít nhất 8 ký tự.', 'warning');
        return;
    }

    // 3. Kiểm tra định dạng mật khẩu (Regex giống Backend)
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!passwordRegex.test(newPassword)) {
        Swal.fire({
            icon: 'warning',
            title: 'Mật khẩu yếu!',
            text: 'Mật khẩu phải bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.'
        });
        return;
    }

    // 4. Kiểm tra khớp mật khẩu (Match Check)
    if (newPassword !== confirmPassword) {
        Swal.fire('Lỗi xác nhận!', 'Mật khẩu mới và Xác nhận không khớp nhau.', 'error');
        return;
    }

    try {
        const response = await fetch('/api/profile/change-password', {
            method: 'POST',
            headers: getCsrfConfig(),
            body: JSON.stringify({ 
                currentPassword: currentPassword, 
                newPassword: newPassword,
                confirmPassword: confirmPassword
            })
        });
        
        const result = await response.json();
        if (response.ok) {
            document.getElementById('passwordForm').reset();
            Swal.fire('Thành công!', 'Đổi mật khẩu thành công. Lần đăng nhập sau hãy dùng mật khẩu mới.', 'success');
        } else {
            // Xử lý lỗi từ backend (Validation hoặc mật khẩu cũ sai)
            let errorMsg = result.message || 'Có lỗi xảy ra.';
            if (typeof result === 'object' && !result.message) {
                // Nếu result là Map chứa các lỗi validation
                errorMsg = Object.values(result).join('<br>');
            }
            
            Swal.fire({
                icon: 'error',
                title: 'Thất bại!',
                html: errorMsg
            });
        }
    } catch (error) {
        console.error("Error:", error);
        Swal.fire('Lỗi kết nối!', 'Không thể kết nối đến máy chủ.', 'error');
    }
}
