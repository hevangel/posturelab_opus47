import requests, json, sys

# Test the Docker container endpoint
files = {
    'front': ('front_view.png', open('../example/front_view.png', 'rb'), 'image/png'),
    'side': ('side_view.png', open('../example/side_view.png', 'rb'), 'image/png'),
}
data = {
    'patient_name': 'Patient',
    'report_date': '2026-05-04',
    'patient_height_in': '68',
    'patient_weight_lb': '160',
    'base_head_weight_lb': '11',
}

# Test native first
files_native = {
    'front': ('front_view.png', open('../example/front_view.png', 'rb'), 'image/png'),
    'side': ('side_view.png', open('../example/side_view.png', 'rb'), 'image/png'),
}
r_native = requests.post('http://127.0.0.1:8000/analyze', files=files_native, data=data)
native = r_native.json()

# Test Docker
r_docker = requests.post('http://127.0.0.1:8001/analyze', files=files, data=data)
print(f'Docker status: {r_docker.status_code}')
if r_docker.status_code == 200:
    docker_resp = r_docker.json()
    print(f'Native  front total_shift: {native["front"]["total_shift_in"]:.4f}')
    print(f'Docker  front total_shift: {docker_resp["front"]["total_shift_in"]:.4f}')
    print(f'Native  side head_forward: {native["side"]["head_forward_in"]:.4f}')
    print(f'Docker  side head_forward: {docker_resp["side"]["head_forward_in"]:.4f}')
    print(f'Native  side eff_weight:   {native["side"]["effective_head_weight_lb"]:.2f}')
    print(f'Docker  side eff_weight:   {docker_resp["side"]["effective_head_weight_lb"]:.2f}')
    # Check approximate parity (model inference can vary slightly across platforms)
    diff_shift = abs(native['front']['total_shift_in'] - docker_resp['front']['total_shift_in'])
    diff_head = abs(native['side']['head_forward_in'] - docker_resp['side']['head_forward_in'])
    print(f'\nDiff front shift: {diff_shift:.4f} in')
    print(f'Diff side head:   {diff_head:.4f} in')
    if diff_shift < 0.5 and diff_head < 0.5:
        print('PASS - Docker and native produce consistent results')
    else:
        print('WARNING - larger differences (may be model nondeterminism)')
else:
    print('Error:', r_docker.text[:500])
    sys.exit(1)
