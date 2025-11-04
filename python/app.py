from flask import Flask, jsonify, request
from flask_cors import CORS
from pulp import LpMaximize, LpProblem, LpVariable, lpSum, LpStatus, value

app = Flask(__name__)
CORS(app)  # Enable CORS for Spring Boot connection


@app.route("/propose-campaign", methods=["POST"])
def propose_campaign():
    """
    Endpoint chính để đề xuất chiến dịch marketing tối ưu
    
    Input: JSON với total_budget, target_audience, available_actions, optimizationGoal
    
    Optimization Goals:
    - "ROI" (mặc định): Tối đa hóa lợi nhuận ròng (Revenue Saved - Cost)
    - "CONVERSION": Tối đa hóa số lượng người được chuyển đổi thành công
    - "CPC": Tối thiểu hóa chi phí trên mỗi conversion (Cost Per Conversion)
    
    Output: JSON với recommended_plan (ROI, cost, retention, distribution)
    """
    try:
        # Nhận dữ liệu từ request
        data = request.json
        
        if not data:
            return jsonify({"error": "No data provided"}), 400
        
        # Trích xuất dữ liệu đầu vào
        total_budget = data.get("total_budget", 0)
        target_audience = data.get("target_audience", [])
        available_actions = data.get("available_actions", [])
        optimization_goal = data.get("optimizationGoal", "ROI")  # Mặc định là ROI
        
        # Validate input
        if total_budget <= 0:
            return jsonify({"error": "Invalid total_budget"}), 400
        if not target_audience:
            return jsonify({"error": "Empty target_audience"}), 400
        if not available_actions:
            return jsonify({"error": "Empty available_actions"}), 400
        
        # Log để debug
        print(f"Đã nhận được Budget: {total_budget}")
        print(f"Số lượng khách hàng: {len(target_audience)}")
        print(f"Số lượng hành động: {len(available_actions)}")
        print(f"Mục tiêu tối ưu hóa: {optimization_goal}")
        
        # Tính toán đầu vào phụ
        total_audience_size = len(target_audience)
        avg_revenue_per_customer = sum(customer.get("avg_revenue", 0) 
                                       for customer in target_audience) / total_audience_size
        
        print(f"Doanh thu TB/khách hàng: {avg_revenue_per_customer:.2f}")
        
        # Khởi tạo mô hình tối ưu hóa
        # Sense sẽ được xác định dựa trên optimization_goal
        if optimization_goal == "CPC":
            model = LpProblem(name="campaign-optimization", sense=LpMaximize)  # Sẽ đảo dấu để minimize
        else:
            model = LpProblem(name="campaign-optimization", sense=LpMaximize)
        
        # Định nghĩa biến quyết định (số lượng khách hàng cho mỗi hành động)
        decision_vars = {}
        for action in available_actions:
            action_id = action.get("action_id")
            # Biến số nguyên không âm
            decision_vars[action_id] = LpVariable(
                name=f"x_{action_id}", 
                lowBound=0, 
                cat='Integer'
            )
        
        # Hàm mục tiêu: Phụ thuộc vào optimizationGoal
        if optimization_goal == "CONVERSION":
            # Mục tiêu 1: Tối đa hóa số người được chuyển đổi (conversion)
            print("📊 Sử dụng hàm mục tiêu: Maximize Total Conversions")
            objective = []
            for action in available_actions:
                action_id = action.get("action_id")
                success_rate = action.get("success_rate", 0)
                # Tối đa hóa tổng số người được chuyển đổi thành công
                objective.append(decision_vars[action_id] * success_rate)
            
            model += lpSum(objective), "Maximize_Total_Conversions"
        
        elif optimization_goal == "CPC":
            # Mục tiêu 2: Tối thiểu hóa Cost Per Conversion (CPC)
            # CPC = Total Cost / Total Conversions
            # Để tránh chia trong LP, ta maximize: Total Conversions / Total Cost
            # Hoặc đơn giản hơn: minimize Total Cost với ràng buộc conversion tối thiểu
            print("📊 Sử dụng hàm mục tiêu: Minimize Cost Per Conversion")
            
            # Để đơn giản, ta sẽ maximize (-Total Cost) tức là minimize Total Cost
            # Và thêm ràng buộc để đảm bảo có đủ conversion
            objective = []
            for action in available_actions:
                action_id = action.get("action_id")
                cost_per_user = action.get("cost_per_user", 0)
                # Minimize cost = Maximize (-cost)
                objective.append(decision_vars[action_id] * (-cost_per_user))
            
            model += lpSum(objective), "Minimize_Total_Cost"
            
            # Thêm ràng buộc: Phải có ít nhất một số conversion tối thiểu
            min_conversions = max(1, int(total_audience_size * 0.1))  # Ít nhất 10% audience
            conversion_constraint = []
            for action in available_actions:
                action_id = action.get("action_id")
                success_rate = action.get("success_rate", 0)
                conversion_constraint.append(decision_vars[action_id] * success_rate)
            
            model += lpSum(conversion_constraint) >= min_conversions, "Minimum_Conversion_Constraint"
        
        else:  # Mặc định là ROI
            # Mục tiêu 3: Tối đa hóa ROI (Return on Investment)
            print("📊 Sử dụng hàm mục tiêu: Maximize ROI (Net Profit)")
            objective = []
            for action in available_actions:
                action_id = action.get("action_id")
                cost_per_user = action.get("cost_per_user", 0)
                success_rate = action.get("success_rate", 0)
                
                # Net Profit = Revenue saved - Cost
                net_value = (success_rate * avg_revenue_per_customer) - cost_per_user
                objective.append(decision_vars[action_id] * net_value)
            
            model += lpSum(objective), "Maximize_Net_Profit"
        
        # Ràng buộc 1: Ngân sách
        budget_constraint = []
        for action in available_actions:
            action_id = action.get("action_id")
            cost_per_user = action.get("cost_per_user", 0)
            budget_constraint.append(decision_vars[action_id] * cost_per_user)
        
        model += lpSum(budget_constraint) <= total_budget, "Budget_Constraint"
        
        # Ràng buộc 2: Số lượng đối tượng
        audience_constraint = []
        for action in available_actions:
            action_id = action.get("action_id")
            audience_constraint.append(decision_vars[action_id])
        
        model += lpSum(audience_constraint) <= total_audience_size, "Audience_Constraint"
        
        # Giải bài toán
        status = model.solve()
        
        if LpStatus[status] != 'Optimal':
            print(f"Warning: Solution status is {LpStatus[status]}")
        
        # Xử lý kết quả
        # QUAN TRỌNG: Làm tròn từng action TRƯỚC, sau đó cộng lại
        # Để đảm bảo tổng KPI = tổng trong bảng phân bổ
        distribution = []
        total_cost = 0
        total_assigned = 0  # Đây sẽ là tổng của các assigned_count đã làm tròn
        revenue_saved = 0
        
        # Bước 1: Làm tròn từng action và tính toán
        for action in available_actions:
            action_id = action.get("action_id")
            # Làm tròn số lượng khách hàng cho từng action
            assigned_count = int(round(value(decision_vars[action_id]) or 0))
            
            if assigned_count > 0:
                cost_per_user = action.get("cost_per_user", 0)
                success_rate = action.get("success_rate", 0)
                
                # Tính toán dựa trên assigned_count đã làm tròn
                action_cost = assigned_count * cost_per_user
                action_revenue = assigned_count * success_rate * avg_revenue_per_customer
                
                # Cộng dồn các giá trị đã tính từ số đã làm tròn
                total_cost += action_cost
                revenue_saved += action_revenue
                total_assigned += assigned_count  # Cộng số đã làm tròn
                
                # Map action_id to action_name
                action_name_map = {
                    "voucher_20": "Voucher 20%",
                    "voucher_10": "Voucher 10%",
                    "personal_call": "Gọi điện thoại",
                    "email_campaign": "Chiến dịch Email",
                    "sms_reminder": "Nhắc nhở SMS"
                }
                action_name = action_name_map.get(action_id, action_id.replace("_", " ").title())
                
                distribution.append({
                    "action_id": action_id,
                    "action_name": action_name,
                    "assigned_count": assigned_count,  # Số đã làm tròn
                    "action_cost": round(action_cost, 2)
                })
        
        # Bước 2: Tính toán KPI dựa trên total_assigned (tổng các số đã làm tròn)
        predicted_roi = (revenue_saved - total_cost) / total_cost if total_cost > 0 else 0
        # expected_retention dựa trên total_assigned (đã là tổng các số làm tròn)
        expected_retention = int((total_assigned / total_audience_size) * 100) if total_audience_size > 0 else 0
        
        # Logging để verify tính nhất quán
        sum_from_distribution = sum(item["assigned_count"] for item in distribution)
        print(f"✓ Verification: Total Assigned = {total_assigned}, Sum from Distribution = {sum_from_distribution}")
        assert total_assigned == sum_from_distribution, "Tổng KPI phải bằng tổng trong bảng phân bổ!"
        
        # Tạo response JSON
        response = {
            "recommended_plan": {
                "predicted_roi": round(predicted_roi, 3),
                "total_cost": round(total_cost, 2),
                "expected_retention": expected_retention,
                "revenue_saved": round(revenue_saved, 2),
                "distribution": distribution,
                "optimization_goal": optimization_goal  # Trả về mục tiêu đã sử dụng
            }
        }
        
        print(f"ROI: {predicted_roi:.3f}, Cost: {total_cost:.2f}, Revenue Saved: {revenue_saved:.2f}")
        print(f"Total Customers Retained: {total_assigned} (matches distribution sum)")
        
        return jsonify(response), 200
        
    except Exception as e:
        print(f"Error: {str(e)}")
        return jsonify({"error": str(e)}), 500


@app.route("/health", methods=["GET"])
def health_check():
    """Health check endpoint"""
    return jsonify({"status": "healthy", "service": "Campaign Optimization API"}), 200


if __name__ == "__main__":
    print("=" * 60)
    print("🚀 Campaign Optimization API Server")
    print("=" * 60)
    print("📍 Running on: http://localhost:5001")
    print("📌 Endpoint: POST /propose-campaign")
    print("💚 Health Check: GET /health")
    print("=" * 60)
    app.run(host='0.0.0.0', port=5001, debug=True)
